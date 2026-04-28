// ══════════════════════════════════════════════════════════
// PROVEEDORES
// ══════════════════════════════════════════════════════════
let allSuppliers = [], currentSupplierId = null, currentSpId = null;
let priceHistoryChart = null;

async function loadProveedores() {
    await loadProvDashboard();
}

// ── Tabs ─────────────────────────────────────────────────
function showProvTab(tab, el) {
    ['dashboard','lista','analisis','cotizaciones','eventos'].forEach(t => {
        const tabEl = document.getElementById('prov-tab-' + t);
        if(tabEl) tabEl.classList.toggle('d-none', t !== tab);
    });
    document.querySelectorAll('#prov-tabs .nav-link').forEach(l => l.classList.remove('active'));
    el.classList.add('active');
    if(tab === 'dashboard') loadProvDashboard();
    else if(tab === 'lista') loadSuppliersList();
    else if(tab === 'analisis') loadAnalysisTab();
    else if(tab === 'cotizaciones') loadCotizacionesTab();
    else if(tab === 'eventos') loadEventos();
}

// ── Dashboard ─────────────────────────────────────────────
async function loadProvDashboard() {
    try {
        const d = await api.getSupplierDashboard();
        document.getElementById('prov-stat-total').textContent = d.totalSuppliers;
        document.getElementById('prov-stat-alerts').textContent = d.activeAlerts;
        document.getElementById('prov-stat-opps').textContent = d.buyOpportunities;
        document.getElementById('prov-stat-margin').textContent = d.marginAtRisk;

        const critEl = document.getElementById('prov-critical-alerts');
        if(!d.criticalAlerts.length) {
            critEl.innerHTML = '<div class="list-group-item text-muted small text-center py-3">Sin alertas cr&iacute;ticas</div>';
        } else {
            critEl.innerHTML = d.criticalAlerts.map(function(a) {
                return '<div class="list-group-item list-group-item-action py-2 px-3">' +
                    '<div class="d-flex justify-content-between align-items-start">' +
                    '<div><span class="badge ' + alertBadgeClass(a.type) + ' me-1">' + a.type.replace(/_/g,' ') + '</span>' +
                    '<span class="small fw-semibold">' + a.supplierName + ' &mdash; ' + a.productName + '</span></div>' +
                    '<button class="btn btn-outline-success btn-sm py-0 px-1" onclick="resolveSupplierAlert(' + a.id + ')"><i class="bi bi-check2"></i></button>' +
                    '</div><div class="text-muted" style="font-size:.75rem;margin-top:2px">' + a.message + '</div></div>';
            }).join('');
        }

        const evEl = document.getElementById('prov-upcoming-events');
        if(!d.upcomingEvents.length) {
            evEl.innerHTML = '<div class="list-group-item text-muted small text-center py-3">Sin eventos pr&oacute;ximos</div>';
        } else {
            evEl.innerHTML = d.upcomingEvents.map(function(e) {
                return '<div class="list-group-item py-2 px-3">' +
                    '<div class="d-flex justify-content-between">' +
                    '<span class="badge ' + eventBadgeClass(e.intensity) + ' me-1">' + e.type.replace(/_/g,' ') + '</span>' +
                    '<span class="small text-muted">' + e.startDate + (e.endDate ? ' &rarr; ' + e.endDate : '') + '</span></div>' +
                    '<div class="small mt-1">' + e.description + '</div>' +
                    (e.expectedImpact ? '<div class="text-muted" style="font-size:.75rem">' + e.expectedImpact.replace(/_/g,' ') + '</div>' : '') +
                    '</div>';
            }).join('');
        }
    } catch(e) { console.error(e); }
}

function alertBadgeClass(type) {
    var map = { SUBIDA_PRECIO:'bg-danger', TENDENCIA_ALCISTA:'bg-danger', MINIMO_HISTORICO:'bg-success',
                PREDICCION_SUBIDA:'bg-warning text-dark', SIN_COTIZAR:'bg-secondary', MARGEN_EN_RIESGO:'bg-danger' };
    return map[type] || 'bg-secondary';
}
function eventBadgeClass(intensity) {
    return intensity === 'ALTA' ? 'bg-danger' : intensity === 'MEDIA' ? 'bg-warning text-dark' : 'bg-secondary';
}

async function resolveSupplierAlert(id) {
    try {
        await api.resolveSupplierAlert(id);
        toast('Alerta resuelta');
        loadProvDashboard();
    } catch(e) { toast(e.message, 'danger'); }
}

// ── Suppliers List ────────────────────────────────────────
async function loadSuppliersList() {
    try {
        allSuppliers = await api.getSuppliers();
        renderSuppliersList();
    } catch(e) { console.error(e); }
}

function renderSuppliersList() {
    var tbody = document.getElementById('suppliers-tbody');
    if(!allSuppliers.length) { tbody.innerHTML = emptyRow(9,'Sin proveedores registrados'); return; }
    tbody.innerHTML = allSuppliers.map(function(s) {
        var stars = '&#9733;'.repeat(Math.round(s.rating||0)) + '&#9734;'.repeat(5-Math.round(s.rating||0));
        return '<tr>' +
            '<td class="fw-semibold">' + s.name + '</td>' +
            '<td>' + (s.nit||'&mdash;') + '</td>' +
            '<td>' + (s.city||'&mdash;') + '</td>' +
            '<td>' + (s.contactName||'&mdash;') + '<br><small class="text-muted">' + (s.phone||'') + '</small></td>' +
            '<td>' + s.paymentTermsDays + ' d&iacute;as</td>' +
            '<td>' + s.leadTimeDays + ' d&iacute;as</td>' +
            '<td>' + stars + '</td>' +
            '<td><span class="badge bg-primary rounded-pill">' + s.productCount + '</span></td>' +
            '<td>' +
            '<button class="btn btn-sm btn-outline-primary py-0 px-1 me-1" onclick="viewSupplierProducts(' + s.id + ',this)" title="Ver productos"><i class="bi bi-box-seam"></i></button>' +
            '<button class="btn btn-sm btn-outline-secondary py-0 px-1 me-1" onclick="openSupplierModal(' + s.id + ')"><i class="bi bi-pencil"></i></button>' +
            '<button class="btn btn-sm btn-outline-danger py-0 px-1" onclick="deleteSupplier(' + s.id + ')"><i class="bi bi-trash"></i></button>' +
            '</td></tr>';
    }).join('');
}

async function viewSupplierProducts(supplierId, btnEl) {
    currentSupplierId = supplierId;
    var s = allSuppliers.find(function(x){ return x.id === supplierId; });
    document.getElementById('selected-supplier-name').textContent = s ? s.name : '';
    document.getElementById('supplier-products-panel').classList.remove('d-none');
    try {
        var sps = await api.getSupplierProducts(supplierId);
        var tbody = document.getElementById('supplier-products-tbody');
        if(!sps.length) { tbody.innerHTML = emptyRow(7,'Sin productos'); return; }
        tbody.innerHTML = sps.map(function(sp) {
            return '<tr>' +
                '<td class="fw-semibold">' + sp.productName + '</td>' +
                '<td><code>' + (sp.supplierProductCode||'&mdash;') + '</code></td>' +
                '<td>' + fmt(sp.currentPrice) + '</td>' +
                '<td class="text-success">' + (sp.minHistoricalPrice ? fmt(sp.minHistoricalPrice) : '&mdash;') + '</td>' +
                '<td class="text-danger">' + (sp.maxHistoricalPrice ? fmt(sp.maxHistoricalPrice) : '&mdash;') + '</td>' +
                '<td>' + (sp.lastQuoteDate ? fmtDate(sp.lastQuoteDate) : '<span class="text-muted">Sin cotizar</span>') + '</td>' +
                '<td>' +
                '<button class="btn btn-sm btn-outline-info py-0 px-1 me-1" onclick="viewAnalysisDetail(' + sp.id + ',\'' + sp.supplierName.replace(/'/g,"\\'") + ' &mdash; ' + sp.productName.replace(/'/g,"\\'") + '\')" title="An&aacute;lisis"><i class="bi bi-graph-up"></i></button>' +
                '<button class="btn btn-sm btn-outline-success py-0 px-1 me-1" onclick="openQuoteModalForSp(' + sp.id + ',\'' + sp.supplierName.replace(/'/g,"\\'") + ' &mdash; ' + sp.productName.replace(/'/g,"\\'") + '\')" title="Cotizar"><i class="bi bi-receipt"></i></button>' +
                '<button class="btn btn-sm btn-outline-danger py-0 px-1" onclick="deleteSupplierProduct(' + sp.id + ')" title="Eliminar"><i class="bi bi-trash"></i></button>' +
                '</td></tr>';
        }).join('');
    } catch(e) { console.error(e); }
}

// ── Supplier CRUD ─────────────────────────────────────────
function openSupplierModal(id) {
    document.getElementById('supplier-id').value = '';
    ['name','nit','contact','email','phone','city','address'].forEach(function(f) {
        document.getElementById('supplier-' + f).value = '';
    });
    document.getElementById('supplier-notes').value = '';
    document.getElementById('supplier-payment').value = 0;
    document.getElementById('supplier-lead').value = 1;
    document.getElementById('supplier-rating').value = 3;
    document.getElementById('modal-supplier-title').textContent = id ? 'Editar Proveedor' : 'Nuevo Proveedor';
    if(id) {
        var s = allSuppliers.find(function(x){ return x.id === id; });
        if(s) {
            document.getElementById('supplier-id').value = s.id;
            document.getElementById('supplier-name').value = s.name;
            document.getElementById('supplier-nit').value = s.nit||'';
            document.getElementById('supplier-contact').value = s.contactName||'';
            document.getElementById('supplier-email').value = s.email||'';
            document.getElementById('supplier-phone').value = s.phone||'';
            document.getElementById('supplier-city').value = s.city||'';
            document.getElementById('supplier-address').value = s.address||'';
            document.getElementById('supplier-payment').value = s.paymentTermsDays||0;
            document.getElementById('supplier-lead').value = s.leadTimeDays||1;
            document.getElementById('supplier-rating').value = s.rating||3;
            document.getElementById('supplier-notes').value = s.notes||'';
        }
    }
    getModal('modalSupplier').show();
}

async function saveSupplier() {
    var id = document.getElementById('supplier-id').value;
    var data = {
        name: document.getElementById('supplier-name').value.trim(),
        nit: document.getElementById('supplier-nit').value.trim(),
        contactName: document.getElementById('supplier-contact').value.trim(),
        email: document.getElementById('supplier-email').value.trim(),
        phone: document.getElementById('supplier-phone').value.trim(),
        city: document.getElementById('supplier-city').value.trim(),
        address: document.getElementById('supplier-address').value.trim(),
        paymentTermsDays: +document.getElementById('supplier-payment').value,
        leadTimeDays: +document.getElementById('supplier-lead').value,
        rating: +document.getElementById('supplier-rating').value,
        notes: document.getElementById('supplier-notes').value.trim()
    };
    try {
        if(id) await api.updateSupplier(id, data); else await api.createSupplier(data);
        getModal('modalSupplier').hide();
        toast(id ? 'Proveedor actualizado' : 'Proveedor creado');
        loadSuppliersList();
    } catch(e) { toast(e.message, 'danger'); }
}

async function deleteSupplier(id) {
    if(!confirm('Desactivar este proveedor?')) return;
    try {
        await api.deleteSupplier(id);
        toast('Proveedor desactivado');
        loadSuppliersList();
        document.getElementById('supplier-products-panel').classList.add('d-none');
    } catch(e) { toast(e.message, 'danger'); }
}

// ── Supplier Products ─────────────────────────────────────
async function openSupplierProductModal() {
    document.getElementById('sp-supplier-id').value = currentSupplierId;
    document.getElementById('sp-price').value = '';
    document.getElementById('sp-min-qty').value = 1;
    document.getElementById('sp-code').value = '';
    document.getElementById('sp-preferred').checked = false;
    var sel = document.getElementById('sp-product-id');
    sel.innerHTML = '<option value="">— Selecciona —</option>' + allProducts.map(function(p) {
        return '<option value="' + p.id + '">' + p.name + '</option>';
    }).join('');
    getModal('modalSupplierProduct').show();
}

async function saveSupplierProduct() {
    var data = {
        supplierId: +document.getElementById('sp-supplier-id').value,
        productId: +document.getElementById('sp-product-id').value,
        currentPrice: +document.getElementById('sp-price').value || null,
        minOrderQuantity: +document.getElementById('sp-min-qty').value,
        supplierProductCode: document.getElementById('sp-code').value.trim(),
        preferred: document.getElementById('sp-preferred').checked
    };
    try {
        await api.addSupplierProduct(data);
        getModal('modalSupplierProduct').hide();
        toast('Producto agregado al proveedor');
        viewSupplierProducts(currentSupplierId, null);
    } catch(e) { toast(e.message, 'danger'); }
}

async function deleteSupplierProduct(spId) {
    if(!confirm('Quitar este producto del proveedor?')) return;
    try {
        await api.deleteSupplierProduct(spId);
        toast('Producto eliminado del proveedor');
        viewSupplierProducts(currentSupplierId, null);
    } catch(e) { toast(e.message, 'danger'); }
}

// ── Analysis Tab ──────────────────────────────────────────
async function loadAnalysisTab() {
    var sel = document.getElementById('analysis-product-select');
    sel.innerHTML = '<option value="">— Selecciona un producto —</option>' + allProducts.map(function(p) {
        return '<option value="' + p.id + '">' + p.name + '</option>';
    }).join('');
    document.getElementById('comparison-table-container').classList.add('d-none');
    document.getElementById('analysis-detail').classList.add('d-none');
}

async function loadSupplierComparison() {
    var productId = document.getElementById('analysis-product-select').value;
    if(!productId) return;
    document.getElementById('comparison-table-container').classList.remove('d-none');
    document.getElementById('analysis-detail').classList.add('d-none');
    try {
        var results = await api.compareSuppliers(productId);
        var tbody = document.getElementById('comparison-tbody');
        if(!results.length) { tbody.innerHTML = emptyRow(8,'Sin proveedores para este producto'); return; }
        tbody.innerHTML = results.map(function(a, i) {
            var trendIcon = a.trend === 'SUBIDA' ? '&uarr; <span class="text-danger">SUBIDA</span>' :
                            a.trend === 'BAJADA' ? '&darr; <span class="text-success">BAJADA</span>' : '&rarr; ESTABLE';
            var flags = [
                a.isBuyOpportunity ? '<span class="badge bg-success">COMPRAR</span>' : '',
                a.isMarginAtRisk ? '<span class="badge bg-warning text-dark">MARGEN</span>' : '',
                a.isStale ? '<span class="badge bg-secondary">DESACTUALIZADO</span>' : ''
            ].filter(Boolean).join(' ');
            var scoreColor = a.score >= 70 ? 'success' : a.score >= 40 ? 'warning' : 'danger';
            var spNameSafe = (a.supplierName + ' -- ' + a.productName).replace(/'/g, '');
            return '<tr class="' + (i===0?'table-success':'') + '">' +
                '<td><strong>' + a.supplierName + '</strong></td>' +
                '<td>' + fmt(a.currentPrice) + '</td>' +
                '<td class="text-success">' + (a.minHistorical ? fmt(a.minHistorical) : '&mdash;') + '</td>' +
                '<td>' + trendIcon + ' (' + a.trendPercent + '%)</td>' +
                '<td>' + (a.volatility||'0.00') + '</td>' +
                '<td>' + (+a.predictedPrice30Days > 0 ? fmt(a.predictedPrice30Days) + ' <small class="text-muted">(' + a.predictionConfidence + ')</small>' : '&mdash;') + '</td>' +
                '<td><span class="badge bg-' + scoreColor + ' rounded-pill fs-6">' + a.score + '</span></td>' +
                '<td>' + (flags||'&mdash;') + ' <button class="btn btn-outline-info btn-sm py-0 px-1 ms-1" onclick="viewAnalysisDetail(' + a.supplierProductId + ',\'' + spNameSafe + '\')"><i class="bi bi-graph-up"></i></button></td>' +
                '</tr>';
        }).join('');
    } catch(e) { toast(e.message, 'danger'); }
}

async function viewAnalysisDetail(spId, label) {
    document.getElementById('analysis-detail').classList.remove('d-none');
    document.getElementById('analysis-sp-name').textContent = label;
    try {
        var a = await api.analyzePrices(spId);
        var confColor = a.predictionConfidence === 'ALTA' ? 'success' : a.predictionConfidence === 'MEDIA' ? 'warning' : 'secondary';
        var ind = document.getElementById('analysis-indicators');
        ind.innerHTML =
            '<div class="mb-2"><div class="text-muted">Precio actual</div><div class="fw-bold fs-5">' + fmt(a.currentPrice) + '</div></div>' +
            '<div class="mb-2"><div class="text-muted">Precio venta</div><div>' + fmt(a.salePrice) + ' <span class="text-muted">(margen ' + a.marginPercent + '%)</span></div></div>' +
            '<div class="mb-2"><div class="text-muted">MA7 / MA30</div><div>' + fmt(a.ma7) + ' / ' + fmt(a.ma30) + '</div></div>' +
            '<div class="mb-2"><div class="text-muted">M&iacute;n / M&aacute;x hist&oacute;rico</div><div>' +
            '<span class="text-success">' + (a.minHistorical ? fmt(a.minHistorical) : '&mdash;') + '</span> / ' +
            '<span class="text-danger">' + (a.maxHistorical ? fmt(a.maxHistorical) : '&mdash;') + '</span></div></div>' +
            '<div class="mb-2"><div class="text-muted">Volatilidad</div><div>' + (a.volatility||'0.00') + '</div></div>' +
            '<div class="mb-2"><div class="text-muted">Predicci&oacute;n 30 d&iacute;as</div><div>' +
            (+a.predictedPrice30Days > 0 ? fmt(a.predictedPrice30Days) + ' (' + a.predictedChangePercent + '%) ' : '&mdash; ') +
            '<span class="badge bg-' + confColor + '">' + a.predictionConfidence + '</span></div></div>' +
            '<div class="mb-2"><div class="text-muted">Score proveedor</div><div class="fw-bold fs-5 text-' + (a.score>=70?'success':a.score>=40?'warning':'danger') + '">' + a.score + ' / 100</div></div>' +
            '<div class="d-flex gap-1 flex-wrap mt-2">' +
            (a.isBuyOpportunity ? '<span class="badge bg-success">OPORTUNIDAD DE COMPRA</span>' : '') +
            (a.isMarginAtRisk ? '<span class="badge bg-warning text-dark">MARGEN EN RIESGO</span>' : '') +
            (a.isStale ? '<span class="badge bg-secondary">SIN COTIZAR +45 D&Iacute;AS</span>' : '') +
            '</div>';

        if(priceHistoryChart) priceHistoryChart.destroy();
        var ctx = document.getElementById('price-history-chart').getContext('2d');
        priceHistoryChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: (a.priceHistory||[]).map(function(p){ return p.date; }),
                datasets: [{
                    label: 'Precio',
                    data: (a.priceHistory||[]).map(function(p){ return p.price; }),
                    borderColor: '#0d6efd', backgroundColor: 'rgba(13,110,253,0.08)',
                    pointRadius: 4, tension: 0.3, fill: true
                }]
            },
            options: { responsive: true, plugins: { legend: { display: false } }, scales: { y: { beginAtZero: false } } }
        });
    } catch(e) { toast(e.message, 'danger'); }
}

// ── Cotizaciones Tab ──────────────────────────────────────
async function loadCotizacionesTab() {
    var sel = document.getElementById('quote-sp-select');
    sel.innerHTML = '<option value="">— Selecciona proveedor-producto —</option>';
    try {
        var suppliers = await api.getSuppliers();
        for(var i = 0; i < suppliers.length; i++) {
            var s = suppliers[i];
            var sps = await api.getSupplierProducts(s.id);
            sps.forEach(function(sp) {
                var opt = document.createElement('option');
                opt.value = sp.id;
                opt.textContent = s.name + ' — ' + sp.productName;
                sel.appendChild(opt);
            });
        }
    } catch(e) {}
}

async function loadQuotes() {
    var spId = document.getElementById('quote-sp-select').value;
    document.getElementById('btn-add-quote').disabled = !spId;
    if(!spId) { document.getElementById('quotes-tbody').innerHTML = emptyRow(6,'Selecciona una relaci&oacute;n'); return; }
    currentSpId = spId;
    try {
        var quotes = await api.getQuotes(spId);
        var tbody = document.getElementById('quotes-tbody');
        if(!quotes.length) { tbody.innerHTML = emptyRow(6,'Sin cotizaciones'); return; }
        tbody.innerHTML = quotes.map(function(q) {
            var vp = q.variationPercent;
            var varHtml = vp != null ? '<span class="' + (+vp > 0 ? 'text-danger' : +vp < 0 ? 'text-success' : '') + '">' + (+vp > 0 ? '+' : '') + vp + '%</span>' : '&mdash;';
            return '<tr>' +
                '<td>' + fmtDate(q.date) + '</td>' +
                '<td class="fw-semibold">' + fmt(q.price) + '</td>' +
                '<td>' + varHtml + '</td>' +
                '<td><span class="badge bg-secondary">' + q.origin + '</span></td>' +
                '<td>' + (q.note||'&mdash;') + '</td>' +
                '<td>' + (q.valid ? '<span class="badge bg-success">V&aacute;lida</span>' : '<span class="badge bg-secondary">Excluida</span>') + '</td>' +
                '</tr>';
        }).join('');
    } catch(e) { toast(e.message, 'danger'); }
}

function openQuoteModal() {
    if(!currentSpId) return;
    var label = document.getElementById('quote-sp-select').selectedOptions[0] ? document.getElementById('quote-sp-select').selectedOptions[0].text : '';
    openQuoteModalForSp(currentSpId, label);
}

function openQuoteModalForSp(spId, label) {
    currentSpId = spId;
    document.getElementById('quote-sp-label').textContent = label || '';
    document.getElementById('quote-price').value = '';
    document.getElementById('quote-note').value = '';
    getModal('modalQuote').show();
}

async function saveQuote() {
    var data = {
        price: +document.getElementById('quote-price').value,
        note: document.getElementById('quote-note').value.trim()
    };
    try {
        await api.addQuote(currentSpId, data);
        getModal('modalQuote').hide();
        toast('Cotizaci\u00f3n registrada — alertas verificadas');
        loadQuotes();
    } catch(e) { toast(e.message, 'danger'); }
}

// ── Eventos ───────────────────────────────────────────────
async function loadEventos() {
    try {
        var events = await api.getSupplierEvents();
        var tbody = document.getElementById('events-tbody');
        if(!events.length) { tbody.innerHTML = emptyRow(7,'Sin eventos'); return; }
        tbody.innerHTML = events.map(function(e) {
            return '<tr class="' + (e.active ? 'table-warning' : '') + '">' +
                '<td><span class="badge ' + eventBadgeClass(e.intensity) + '">' + e.type.replace(/_/g,' ') + '</span></td>' +
                '<td>' + e.description + '</td>' +
                '<td>' + (e.startDate||'&mdash;') + '</td>' +
                '<td>' + (e.endDate||'Sin fin') + '</td>' +
                '<td>' + (e.expectedImpact ? e.expectedImpact.replace(/_/g,' ') : '&mdash;') + '</td>' +
                '<td><span class="badge ' + eventBadgeClass(e.intensity) + '">' + e.intensity + '</span></td>' +
                '<td>' + (e.active ? '<span class="badge bg-warning text-dark">Activo</span>' : '<span class="badge bg-secondary">Inactivo</span>') + '</td>' +
                '</tr>';
        }).join('');
    } catch(e) { toast(e.message, 'danger'); }
}

function openEventModal() {
    document.getElementById('event-type').value = 'TEMPORADA_ALTA';
    document.getElementById('event-intensity').value = 'MEDIA';
    document.getElementById('event-desc').value = '';
    document.getElementById('event-start').value = '';
    document.getElementById('event-end').value = '';
    document.getElementById('event-impact').value = '';
    getModal('modalEvent').show();
}

async function saveEvent() {
    var data = {
        type: document.getElementById('event-type').value,
        description: document.getElementById('event-desc').value.trim(),
        startDate: document.getElementById('event-start').value,
        endDate: document.getElementById('event-end').value || null,
        expectedImpact: document.getElementById('event-impact').value || null,
        intensity: document.getElementById('event-intensity').value
    };
    try {
        await api.createSupplierEvent(data);
        getModal('modalEvent').hide();
        toast('Evento creado');
        loadEventos();
    } catch(e) { toast(e.message, 'danger'); }
}
