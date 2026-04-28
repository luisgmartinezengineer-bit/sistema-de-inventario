package com.Luis.task_manager.service;

import com.Luis.task_manager.entity.AuditLog;
import com.Luis.task_manager.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xddf.usermodel.chart.*;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuditExcelExportService {

    private final AuditLogRepository auditLogRepository;

    private static final DateTimeFormatter DT_FMT  = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    // ── Paleta corporativa ──────────────────────────────────────────
    private static final byte[] COLOR_NAVY   = hex("1A2540");
    private static final byte[] COLOR_BLUE   = hex("0D6EFD");
    private static final byte[] COLOR_SUCCESS= hex("198754");
    private static final byte[] COLOR_WARN   = hex("FFC107");
    private static final byte[] COLOR_DANGER = hex("DC3545");
    private static final byte[] COLOR_INFO   = hex("0DCAF0");
    private static final byte[] COLOR_LIGHT  = hex("F8F9FA");
    private static final byte[] COLOR_GREY   = hex("6C757D");
    private static final byte[] COLOR_WHITE  = hex("FFFFFF");
    private static final byte[] COLOR_HEADER_ALT = hex("E9ECEF");

    public byte[] generate(String username, String entityType, String action,
                           LocalDate from, LocalDate to) throws Exception {

        LocalDateTime fromDt = from != null ? from.atStartOfDay()      : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59)    : null;
        String u = blank(username) ? null : username;
        String e = blank(entityType) ? null : entityType;
        String a = blank(action) ? null : action;

        List<AuditLog> logs = auditLogRepository
                .findWithFilters(u, e, a, fromDt, toDt, PageRequest.of(0, 5000))
                .getContent();

        try (XSSFWorkbook wb = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // ── hojas ──────────────────────────────────────────────
            buildResumenSheet(wb, logs, from, to);
            buildDetalleSheet(wb, logs);
            buildAnalisisSheet(wb, logs);

            wb.write(out);
            return out.toByteArray();
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // HOJA 1 — RESUMEN EJECUTIVO
    // ═══════════════════════════════════════════════════════════════
    private void buildResumenSheet(XSSFWorkbook wb, List<AuditLog> logs,
                                   LocalDate from, LocalDate to) {
        XSSFSheet sheet = wb.createSheet("Resumen Ejecutivo");
        sheet.setColumnWidth(0, 3000);
        for (int i = 1; i <= 8; i++) sheet.setColumnWidth(i, 3800);

        // ── encabezado principal ────────────────────────────────
        mergeAndWrite(sheet, wb, 0, 0, 0, 8, "SISTEMA DE INVENTARIO — AUDITORÍA CONTABLE",
                headerStyle(wb, COLOR_NAVY, COLOR_WHITE, 18, true), 1000);

        String periodo = (from != null ? DATE_FMT.format(from) : "Inicio")
                + "  →  " + (to != null ? DATE_FMT.format(to) : "Hoy");
        mergeAndWrite(sheet, wb, 1, 1, 0, 8, "Período: " + periodo,
                headerStyle(wb, COLOR_BLUE, COLOR_WHITE, 11, false), 480);

        mergeAndWrite(sheet, wb, 2, 2, 0, 8,
                "Generado: " + DT_FMT.format(LocalDateTime.now()) + "  |  Registros analizados: " + logs.size(),
                headerStyle(wb, hex("2D3F6E"), COLOR_WHITE, 10, false), 420);

        emptyRow(sheet, 3, 320);

        // ── KPI boxes ──────────────────────────────────────────
        long total   = logs.size();
        long ventas  = logs.stream().filter(l -> "VENTA_CREADA".equals(l.getAction())).count();
        long devol   = logs.stream().filter(l -> l.getAction() != null && l.getAction().startsWith("DEVOLUCION")).count();
        long compras = logs.stream().filter(l -> l.getAction() != null && l.getAction().startsWith("COMPRA")).count();

        writeKpi(sheet, wb, 4, 0, "TOTAL EVENTOS",    String.valueOf(total),   COLOR_NAVY,    1);
        writeKpi(sheet, wb, 4, 2, "VENTAS",           String.valueOf(ventas),  COLOR_SUCCESS, 1);
        writeKpi(sheet, wb, 4, 4, "DEVOLUCIONES",     String.valueOf(devol),   COLOR_WARN,    1);
        writeKpi(sheet, wb, 4, 6, "COMPRAS / ÓRDENES",String.valueOf(compras), COLOR_BLUE,    1);

        emptyRow(sheet, 6, 320);
        emptyRow(sheet, 7, 320);
        emptyRow(sheet, 8, 320);

        // ── tabla por acción (fuente de datos para gráfica) ────
        Map<String, Long> byAction = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getAction() == null ? "OTRO" : l.getAction(), Collectors.counting()));

        int dataRow = 10;
        XSSFRow hdr = sheet.createRow(dataRow);
        setCellStyled(wb, hdr, 0, "Tipo de Acción",    sectionHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, hdr, 1, "Cantidad",           sectionHeaderStyle(wb), CellType.STRING);
        int r = dataRow + 1;
        for (Map.Entry<String, Long> entry : byAction.entrySet()) {
            XSSFRow row = sheet.createRow(r++);
            row.createCell(0).setCellValue(entry.getKey());
            row.createCell(1).setCellValue(entry.getValue());
        }

        // ── tabla por módulo ───────────────────────────────────
        Map<String, Long> byEntity = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getEntityType() == null ? "OTRO" : l.getEntityType(), Collectors.counting()));

        int dataCol = 3;
        XSSFRow hdr2 = sheet.getRow(dataRow);
        setCellStyled(wb, hdr2, dataCol,   "Módulo",    sectionHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, hdr2, dataCol+1, "Cantidad",  sectionHeaderStyle(wb), CellType.STRING);
        int r2 = dataRow + 1;
        for (Map.Entry<String, Long> entry : byEntity.entrySet()) {
            XSSFRow row = sheet.getRow(r2) != null ? sheet.getRow(r2) : sheet.createRow(r2);
            row.createCell(dataCol).setCellValue(entry.getKey());
            row.createCell(dataCol + 1).setCellValue(entry.getValue());
            r2++;
        }

        // ── GRÁFICA CIRCULAR 1: por acción ─────────────────────
        if (byAction.size() >= 2) {
            XSSFDrawing drawing = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor1 = drawing.createAnchor(0, 0, 0, 0, 0, 17, 4, 35);
            buildPieChart(wb, sheet, drawing, anchor1,
                    "Distribución por Tipo de Acción",
                    dataRow + 1, r - 1, 0, 1);
        }

        // ── GRÁFICA CIRCULAR 2: por módulo ─────────────────────
        if (byEntity.size() >= 2) {
            XSSFDrawing drawing2 = sheet.createDrawingPatriarch();
            XSSFClientAnchor anchor2 = drawing2.createAnchor(0, 0, 0, 0, 4, 17, 9, 35);
            buildPieChart(wb, sheet, drawing2, anchor2,
                    "Distribución por Módulo del Sistema",
                    dataRow + 1, r2 - 1, dataCol, dataCol + 1);
        }

        // ── Top usuarios ───────────────────────────────────────
        int topRow = 36;
        mergeAndWrite(sheet, wb, topRow, topRow, 0, 8, "TOP USUARIOS POR ACTIVIDAD",
                sectionHeaderStyle(wb), 420);
        topRow++;

        XSSFRow topHdr = sheet.createRow(topRow++);
        setCellStyled(wb, topHdr, 0, "Usuario",      colHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, topHdr, 1, "Rol",          colHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, topHdr, 2, "N° Eventos",   colHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, topHdr, 3, "% Participación", colHeaderStyle(wb), CellType.STRING);

        logs.stream()
                .collect(Collectors.groupingBy(AuditLog::getUsername, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
                    String uname = entry.getKey();
                    long cnt = entry.getValue();
                    String rol = logs.stream().filter(l -> uname.equals(l.getUsername()) && l.getUserRole() != null)
                            .map(AuditLog::getUserRole).findFirst().orElse("—");
                    double pct = total > 0 ? cnt * 100.0 / total : 0;

                    CellStyle alt = altRowStyle(wb);
                    row.createCell(0).setCellValue(uname);  row.getCell(0).setCellStyle(alt);
                    row.createCell(1).setCellValue(rol);    row.getCell(1).setCellStyle(alt);
                    row.createCell(2).setCellValue(cnt);    row.getCell(2).setCellStyle(numStyle(wb));
                    XSSFCell pctCell = row.createCell(3);
                    pctCell.setCellValue(pct / 100.0);
                    pctCell.setCellStyle(pctStyle(wb));
                });

        sheet.setZoom(90);
    }

    // ═══════════════════════════════════════════════════════════════
    // HOJA 2 — REGISTRO DETALLADO
    // ═══════════════════════════════════════════════════════════════
    private void buildDetalleSheet(XSSFWorkbook wb, List<AuditLog> logs) {
        XSSFSheet sheet = wb.createSheet("Registro Detallado");

        int[] widths = {1500, 4500, 3200, 3200, 4500, 3200, 2000, 7000, 3500};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i]);

        // Título
        mergeAndWrite(sheet, wb, 0, 0, 0, 8,
                "REGISTRO DE TRAZABILIDAD — DETALLE COMPLETO DE EVENTOS",
                headerStyle(wb, COLOR_NAVY, COLOR_WHITE, 13, true), 600);
        emptyRow(sheet, 1, 200);

        // Encabezado columnas
        String[] cols = {"#", "Fecha y Hora", "Usuario", "Rol", "Acción", "Módulo", "Ref. ID", "Detalle Contable", "IP Origen"};
        XSSFRow hdr = sheet.createRow(2);
        hdr.setHeightInPoints(28);
        for (int i = 0; i < cols.length; i++)
            setCellStyled(wb, hdr, i, cols[i], colHeaderStyle(wb), CellType.STRING);

        sheet.createFreezePane(0, 3);
        sheet.setAutoFilter(new CellRangeAddress(2, 2, 0, 8));

        // Filas de datos
        CellStyle dtStyle  = dtCellStyle(wb);
        CellStyle numSt    = numStyle(wb);
        CellStyle normSt   = dataStyle(wb, null);
        CellStyle medioSt  = dataStyle(wb, hex("FFF3CD"));
        CellStyle altoSt   = dataStyle(wb, hex("F8D7DA"));
        CellStyle monoSt   = monoStyle(wb);

        int rowNum = 3;
        for (int i = 0; i < logs.size(); i++) {
            AuditLog l = logs.get(i);
            XSSFRow row = sheet.createRow(rowNum++);
            row.setHeightInPoints(18);

            String sev = severityOf(l.getAction());
            CellStyle rowSt = "ALTO".equals(sev) ? altoSt : "MEDIO".equals(sev) ? medioSt : normSt;

            XSSFCell seqCell = row.createCell(0);
            seqCell.setCellValue(i + 1); seqCell.setCellStyle(numSt);

            XSSFCell tsCell = row.createCell(1);
            tsCell.setCellValue(l.getTimestamp() != null ? DT_FMT.format(l.getTimestamp()) : ""); tsCell.setCellStyle(dtStyle);

            cellData(row, 2, l.getUsername(),   rowSt);
            cellData(row, 3, l.getUserRole() != null ? l.getUserRole() : "—", rowSt);
            cellData(row, 4, l.getAction(),     monoSt);
            cellData(row, 5, l.getEntityType()  != null ? l.getEntityType() : "—", rowSt);

            XSSFCell idCell = row.createCell(6);
            if (l.getEntityId() != null) idCell.setCellValue(l.getEntityId());
            else idCell.setCellValue("—");
            idCell.setCellStyle(numSt);

            cellData(row, 7, l.getDetails()  != null ? l.getDetails()  : "—", rowSt);
            cellData(row, 8, l.getIpAddress()!= null ? l.getIpAddress(): "—", monoSt);
        }

        // Totalizador
        int totalRow = rowNum + 1;
        XSSFRow tot = sheet.createRow(totalRow);
        mergeAndWriteRow(sheet, wb, tot, 0, 6, "TOTAL DE EVENTOS REGISTRADOS:", totalLabelStyle(wb));
        XSSFCell totVal = tot.createCell(7);
        totVal.setCellValue(logs.size());
        totVal.setCellStyle(totalValueStyle(wb));

        sheet.setZoom(85);
    }

    // ═══════════════════════════════════════════════════════════════
    // HOJA 3 — ANÁLISIS ESTADÍSTICO
    // ═══════════════════════════════════════════════════════════════
    private void buildAnalisisSheet(XSSFWorkbook wb, List<AuditLog> logs) {
        XSSFSheet sheet = wb.createSheet("Análisis Estadístico");
        for (int i = 0; i <= 6; i++) sheet.setColumnWidth(i, 4200);

        mergeAndWrite(sheet, wb, 0, 0, 0, 6,
                "ANÁLISIS ESTADÍSTICO DE ACTIVIDAD",
                headerStyle(wb, COLOR_NAVY, COLOR_WHITE, 13, true), 600);
        emptyRow(sheet, 1, 200);

        // ── Tabla: actividad por día (últimos 14 días) ──────────
        mergeAndWrite(sheet, wb, 2, 2, 0, 3, "ACTIVIDAD DIARIA (últimos 14 días)",
                sectionHeaderStyle(wb), 420);

        Map<LocalDate, Long> byDay = logs.stream()
                .filter(l -> l.getTimestamp() != null && l.getTimestamp().isAfter(LocalDateTime.now().minusDays(14)))
                .collect(Collectors.groupingBy(l -> l.getTimestamp().toLocalDate(), Collectors.counting()));

        List<LocalDate> days = new ArrayList<>();
        for (int i = 13; i >= 0; i--) days.add(LocalDate.now().minusDays(i));

        XSSFRow dayHdr = sheet.createRow(3);
        setCellStyled(wb, dayHdr, 0, "Fecha",    colHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, dayHdr, 1, "Eventos",  colHeaderStyle(wb), CellType.STRING);

        int dayRow = 4;
        for (LocalDate d : days) {
            XSSFRow row = sheet.createRow(dayRow++);
            row.createCell(0).setCellValue(DATE_FMT.format(d));
            row.createCell(1).setCellValue(byDay.getOrDefault(d, 0L));
            row.getCell(0).setCellStyle(dataStyle(wb, dayRow % 2 == 0 ? COLOR_HEADER_ALT : COLOR_WHITE));
            row.getCell(1).setCellStyle(numStyle(wb));
        }

        // ── Gráfica de barras: actividad diaria ────────────────
        XSSFDrawing drawing = sheet.createDrawingPatriarch();
        XSSFClientAnchor barAnchor = drawing.createAnchor(0, 0, 0, 0, 3, 3, 9, 20);
        buildBarChart(wb, sheet, drawing, barAnchor,
                "Evolución de Actividad (14 días)", 4, dayRow - 1, 0, 1);

        // ── Tabla: severidad ───────────────────────────────────
        int sevRow = dayRow + 2;
        mergeAndWrite(sheet, wb, sevRow, sevRow, 0, 3, "DISTRIBUCIÓN POR SEVERIDAD",
                sectionHeaderStyle(wb), 420);
        sevRow++;
        XSSFRow sevHdr = sheet.createRow(sevRow++);
        setCellStyled(wb, sevHdr, 0, "Severidad",  colHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, sevHdr, 1, "Eventos",    colHeaderStyle(wb), CellType.STRING);
        setCellStyled(wb, sevHdr, 2, "% del Total",colHeaderStyle(wb), CellType.STRING);

        long total = logs.size();
        Map<String, Long> bySev = new LinkedHashMap<>();
        bySev.put("NORMAL", logs.stream().filter(l -> "NORMAL".equals(severityOf(l.getAction()))).count());
        bySev.put("MEDIO",  logs.stream().filter(l -> "MEDIO".equals(severityOf(l.getAction()))).count());
        bySev.put("ALTO",   logs.stream().filter(l -> "ALTO".equals(severityOf(l.getAction()))).count());
        bySev.put("INFO",   logs.stream().filter(l -> "INFO".equals(severityOf(l.getAction()))).count());

        for (Map.Entry<String, Long> entry : bySev.entrySet()) {
            if (entry.getValue() == 0) continue;
            XSSFRow row = sheet.createRow(sevRow++);
            byte[] color = switch (entry.getKey()) {
                case "NORMAL" -> hex("D1E7DD");
                case "MEDIO"  -> hex("FFF3CD");
                case "ALTO"   -> hex("F8D7DA");
                default       -> hex("CFE2FF");
            };
            CellStyle st = dataStyle(wb, color);
            row.createCell(0).setCellValue(entry.getKey()); row.getCell(0).setCellStyle(st);
            row.createCell(1).setCellValue(entry.getValue()); row.getCell(1).setCellStyle(numStyle(wb));
            XSSFCell pct = row.createCell(2);
            pct.setCellValue(total > 0 ? entry.getValue() * 1.0 / total : 0);
            pct.setCellStyle(pctStyle(wb));
        }

        // ── Gráfica circular severidad ─────────────────────────
        int sevStart = (int)(sevRow - bySev.values().stream().filter(v -> v > 0).count());
        if (bySev.values().stream().filter(v -> v > 0).count() >= 2) {
            XSSFClientAnchor sevAnchor = drawing.createAnchor(0, 0, 0, 0, 3, 21, 7, 37);
            buildPieChart(wb, sheet, drawing, sevAnchor,
                    "Distribución por Severidad de Riesgo",
                    (int) sevStart, sevRow - 1, 0, 1);
        }

        sheet.setZoom(85);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS — GRÁFICAS
    // ═══════════════════════════════════════════════════════════════
    private void buildPieChart(XSSFWorkbook wb, XSSFSheet sheet,
                                XSSFDrawing drawing, XSSFClientAnchor anchor,
                                String title, int firstDataRow, int lastDataRow,
                                int labelCol, int valueCol) {
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.RIGHT);

        XDDFDataSource<String> labels = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new org.apache.poi.ss.util.CellRangeAddress(firstDataRow, lastDataRow, labelCol, labelCol));
        XDDFNumericalDataSource<Double> values = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new org.apache.poi.ss.util.CellRangeAddress(firstDataRow, lastDataRow, valueCol, valueCol));

        XDDFPieChartData data = (XDDFPieChartData) chart.createData(ChartTypes.PIE, null, null);
        data.setFirstSliceAngle(45);
        XDDFPieChartData.Series series = (XDDFPieChartData.Series) data.addSeries(labels, values);
        series.setTitle(title, null);
        series.setExplosion(3L);
        chart.plot(data);

        XSSFChart xssfChart = chart;
        org.openxmlformats.schemas.drawingml.x2006.chart.CTDLbls dLbls =
                xssfChart.getCTChart().getPlotArea().getPieChartArray(0).getSerArray(0).addNewDLbls();
        dLbls.addNewShowVal().setVal(false);
        dLbls.addNewShowLegendKey().setVal(false);
        dLbls.addNewShowCatName().setVal(true);
        dLbls.addNewShowSerName().setVal(false);
        dLbls.addNewShowPercent().setVal(true);
    }

    private void buildBarChart(XSSFWorkbook wb, XSSFSheet sheet,
                                XSSFDrawing drawing, XSSFClientAnchor anchor,
                                String title, int firstDataRow, int lastDataRow,
                                int catCol, int valCol) {
        XSSFChart chart = drawing.createChart(anchor);
        chart.setTitleText(title);
        chart.setTitleOverlay(false);

        XDDFChartLegend legend = chart.getOrAddLegend();
        legend.setPosition(LegendPosition.BOTTOM);

        XDDFCategoryAxis catAxis = chart.createCategoryAxis(AxisPosition.BOTTOM);
        catAxis.setTitle("Fecha");
        XDDFValueAxis valAxis = chart.createValueAxis(AxisPosition.LEFT);
        valAxis.setTitle("N° Eventos");
        valAxis.setCrosses(AxisCrosses.AUTO_ZERO);

        XDDFDataSource<String> cats = XDDFDataSourcesFactory.fromStringCellRange(sheet,
                new org.apache.poi.ss.util.CellRangeAddress(firstDataRow, lastDataRow, catCol, catCol));
        XDDFNumericalDataSource<Double> vals = XDDFDataSourcesFactory.fromNumericCellRange(sheet,
                new org.apache.poi.ss.util.CellRangeAddress(firstDataRow, lastDataRow, valCol, valCol));

        XDDFBarChartData data = (XDDFBarChartData) chart.createData(ChartTypes.BAR, catAxis, valAxis);
        data.setBarDirection(BarDirection.COL);
        XDDFBarChartData.Series series = (XDDFBarChartData.Series) data.addSeries(cats, vals);
        series.setTitle("Eventos por día", null);
        chart.plot(data);
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS — ESTILOS
    // ═══════════════════════════════════════════════════════════════
    private XSSFCellStyle headerStyle(XSSFWorkbook wb, byte[] bg, byte[] fg, int fontSize, boolean bold) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(bg, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(st, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        XSSFFont font = wb.createFont();
        font.setColor(new XSSFColor(fg, null));
        font.setBold(bold);
        font.setFontHeightInPoints((short) fontSize);
        font.setFontName("Calibri");
        st.setFont(font);
        return st;
    }

    private XSSFCellStyle sectionHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(hex("2D3F6E"), null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.LEFT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(st, BorderStyle.THIN, IndexedColors.GREY_50_PERCENT.getIndex());
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(COLOR_WHITE, null));
        f.setBold(true); f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        st.setFont(f);
        return st;
    }

    private XSSFCellStyle colHeaderStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(hex("1A2540"), null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(st, BorderStyle.MEDIUM, IndexedColors.GREY_50_PERCENT.getIndex());
        XSSFFont f = wb.createFont();
        f.setColor(new XSSFColor(COLOR_WHITE, null));
        f.setBold(true); f.setFontHeightInPoints((short) 9); f.setFontName("Calibri");
        st.setFont(f);
        st.setWrapText(false);
        return st;
    }

    private XSSFCellStyle dataStyle(XSSFWorkbook wb, byte[] bg) {
        XSSFCellStyle st = wb.createCellStyle();
        if (bg != null) {
            st.setFillForegroundColor(new XSSFColor(bg, null));
            st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        st.setAlignment(HorizontalAlignment.LEFT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(st, BorderStyle.HAIR, IndexedColors.GREY_25_PERCENT.getIndex());
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 9); f.setFontName("Calibri");
        st.setFont(f);
        return st;
    }

    private XSSFCellStyle altRowStyle(XSSFWorkbook wb) { return dataStyle(wb, COLOR_HEADER_ALT); }

    private XSSFCellStyle monoStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = dataStyle(wb, null);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 8); f.setFontName("Courier New");
        st.setFont(f); return st;
    }

    private XSSFCellStyle dtCellStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = dataStyle(wb, null);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 8); f.setFontName("Courier New");
        st.setFont(f); st.setAlignment(HorizontalAlignment.CENTER); return st;
    }

    private XSSFCellStyle numStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = dataStyle(wb, null);
        st.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont f = wb.createFont(); f.setFontHeightInPoints((short) 9); f.setFontName("Calibri"); f.setBold(true);
        st.setFont(f); return st;
    }

    private XSSFCellStyle pctStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = numStyle(wb);
        st.setDataFormat(wb.createDataFormat().getFormat("0.00%"));
        return st;
    }

    private XSSFCellStyle totalLabelStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(COLOR_NAVY, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.RIGHT);
        st.setVerticalAlignment(VerticalAlignment.CENTER);
        XSSFFont f = wb.createFont(); f.setColor(new XSSFColor(COLOR_WHITE, null));
        f.setBold(true); f.setFontHeightInPoints((short) 10); f.setFontName("Calibri");
        st.setFont(f); setBorder(st, BorderStyle.MEDIUM, IndexedColors.GREY_50_PERCENT.getIndex());
        return st;
    }

    private XSSFCellStyle totalValueStyle(XSSFWorkbook wb) {
        XSSFCellStyle st = wb.createCellStyle();
        st.setFillForegroundColor(new XSSFColor(COLOR_SUCCESS, null));
        st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        st.setAlignment(HorizontalAlignment.CENTER);
        XSSFFont f = wb.createFont(); f.setColor(new XSSFColor(COLOR_WHITE, null));
        f.setBold(true); f.setFontHeightInPoints((short) 12); f.setFontName("Calibri");
        st.setFont(f); setBorder(st, BorderStyle.MEDIUM, IndexedColors.GREY_50_PERCENT.getIndex());
        return st;
    }

    // ═══════════════════════════════════════════════════════════════
    // HELPERS — ESCRITURA / UTILIDADES
    // ═══════════════════════════════════════════════════════════════
    private void writeKpi(XSSFSheet sheet, XSSFWorkbook wb, int rowIdx, int col,
                           String label, String value, byte[] color, int span) {
        sheet.addMergedRegion(new CellRangeAddress(rowIdx, rowIdx, col, col + span));
        sheet.addMergedRegion(new CellRangeAddress(rowIdx + 1, rowIdx + 1, col, col + span));

        XSSFRow r1 = sheet.getRow(rowIdx) != null ? sheet.getRow(rowIdx) : sheet.createRow(rowIdx);
        r1.setHeightInPoints(22);
        XSSFCell labelCell = r1.createCell(col);
        labelCell.setCellValue(label);
        XSSFCellStyle ls = wb.createCellStyle();
        ls.setFillForegroundColor(new XSSFColor(color, null));
        ls.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        ls.setAlignment(HorizontalAlignment.CENTER);
        ls.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(ls, BorderStyle.MEDIUM, IndexedColors.WHITE.getIndex());
        XSSFFont lf = wb.createFont(); lf.setColor(new XSSFColor(COLOR_WHITE, null));
        lf.setFontHeightInPoints((short) 8); lf.setFontName("Calibri");
        ls.setFont(lf); labelCell.setCellStyle(ls);

        XSSFRow r2 = sheet.getRow(rowIdx + 1) != null ? sheet.getRow(rowIdx + 1) : sheet.createRow(rowIdx + 1);
        r2.setHeightInPoints(36);
        XSSFCell valCell = r2.createCell(col);
        valCell.setCellValue(value);
        XSSFCellStyle vs = wb.createCellStyle();
        vs.setFillForegroundColor(new XSSFColor(color, null));
        vs.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        vs.setAlignment(HorizontalAlignment.CENTER);
        vs.setVerticalAlignment(VerticalAlignment.CENTER);
        setBorder(vs, BorderStyle.MEDIUM, IndexedColors.WHITE.getIndex());
        XSSFFont vf = wb.createFont(); vf.setColor(new XSSFColor(COLOR_WHITE, null));
        vf.setBold(true); vf.setFontHeightInPoints((short) 22); vf.setFontName("Calibri");
        vs.setFont(vf); valCell.setCellStyle(vs);
    }

    private void mergeAndWrite(XSSFSheet sheet, XSSFWorkbook wb, int r1, int r2,
                                int c1, int c2, String text, CellStyle style, int height) {
        if (r1 < r2 || c1 < c2) sheet.addMergedRegion(new CellRangeAddress(r1, r2, c1, c2));
        XSSFRow row = sheet.getRow(r1) != null ? sheet.getRow(r1) : sheet.createRow(r1);
        if (height > 0) row.setHeightInPoints(height / 20f);
        XSSFCell cell = row.createCell(c1);
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    private void mergeAndWriteRow(XSSFSheet sheet, XSSFWorkbook wb, XSSFRow row,
                                   int c1, int c2, String text, CellStyle style) {
        if (c1 < c2) sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), c1, c2));
        XSSFCell cell = row.createCell(c1);
        cell.setCellValue(text); cell.setCellStyle(style);
    }

    private void setCellStyled(XSSFWorkbook wb, XSSFRow row, int col, Object value,
                                CellStyle style, CellType type) {
        XSSFCell cell = row.createCell(col);
        if (value instanceof String s) cell.setCellValue(s);
        else if (value instanceof Number n) cell.setCellValue(n.doubleValue());
        cell.setCellStyle(style);
    }

    private void cellData(XSSFRow row, int col, String value, CellStyle style) {
        XSSFCell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void emptyRow(XSSFSheet sheet, int rowNum, int height) {
        XSSFRow row = sheet.createRow(rowNum);
        row.setHeightInPoints(height / 20f);
    }

    private void setBorder(XSSFCellStyle st, BorderStyle bs, short color) {
        st.setBorderTop(bs);    st.setTopBorderColor(color);
        st.setBorderBottom(bs); st.setBottomBorderColor(color);
        st.setBorderLeft(bs);   st.setLeftBorderColor(color);
        st.setBorderRight(bs);  st.setRightBorderColor(color);
    }

    private String severityOf(String action) {
        if (action == null) return "INFO";
        return switch (action) {
            case "COMPRA_CANCELADA", "DEVOLUCION_RECHAZADA" -> "ALTO";
            case "DEVOLUCION_SOLICITADA", "DEVOLUCION_APROBADA" -> "MEDIO";
            case "VENTA_CREADA", "COMPRA_CREADA", "COMPRA_RECIBIDA" -> "NORMAL";
            default -> "INFO";
        };
    }

    private static byte[] hex(String hex) {
        return new byte[]{
            (byte) Integer.parseInt(hex.substring(0, 2), 16),
            (byte) Integer.parseInt(hex.substring(2, 4), 16),
            (byte) Integer.parseInt(hex.substring(4, 6), 16)
        };
    }

    private static boolean blank(String s) { return s == null || s.isBlank(); }
}
