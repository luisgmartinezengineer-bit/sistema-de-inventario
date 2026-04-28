// ── Estado global ─────────────────────────────────────────
let allProducts = [], allCategories = [], allCustomers = [], allUsers = [];
let saleItems = [];
let currentSale = null;
let currentUser = null;
let empresaConfig = null;

// ── Helpers ───────────────────────────────────────────────
const fmt = n => '$' + Number(n || 0).toLocaleString('es-MX', { minimumFractionDigits: 2 });
const fmtDate = d => new Date(d).toLocaleString('es-MX', { dateStyle: 'short', timeStyle: 'short' });
const emptyRow = (cols, msg) => `<tr><td colspan="${cols}" class="text-center text-muted py-4">${msg}</td></tr>`;

function toast(msg, type = 'success') {
    const el = document.getElementById('app-toast');
    el.className = `toast align-items-center text-white border-0 bg-${type}`;
    document.getElementById('toast-msg').textContent = msg;
    bootstrap.Toast.getOrCreateInstance(el, { delay: 3500 }).show();
}
function getModal(id) { return bootstrap.Modal.getOrCreateInstance(document.getElementById(id)); }

// ── Navegación ────────────────────────────────────────────
const SECTIONS = ['dashboard','inventario','ventas','devoluciones','compras','clientes','cajas','categorias','alertas','contabilidad','proveedores','docs','auditoria','usuarios','empresa'];
const TITLES = { dashboard:'Dashboard', inventario:'Productos', ventas:'Ventas', devoluciones:'Devoluciones', compras:'Órdenes de Compra', clientes:'Clientes', cajas:'Cajas', categorias:'Categorías', alertas:'Alertas de Stock', contabilidad:'Contabilidad', proveedores:'Proveedores', docs:'Documentación Técnica', auditoria:'Auditoría', usuarios:'Usuarios', empresa:'Mi Empresa' };
const LOADERS = { dashboard:loadDashboard, inventario:loadInventario, ventas:loadVentas, devoluciones:loadDevoluciones, compras:loadCompras, clientes:loadClientes, cajas:loadCajas, categorias:loadCategorias, alertas:loadAlertas, contabilidad:initContabilidad, proveedores:()=>loadProveedores(), auditoria:loadAuditLogs, usuarios:loadUsuarios, empresa:loadEmpresa };

function showSection(name, el) {
    SECTIONS.forEach(s => document.getElementById(`section-${s}`).classList.toggle('d-none', s !== name));
    document.querySelectorAll('.sidebar-link, .sidebar-sublink').forEach(l => l.classList.remove('active'));
    el.classList.add('active');
    // marcar el grupo padre como activo si el link es un sublink
    const parentSubmenu = el.closest('.collapse');
    if (parentSubmenu) {
        bootstrap.Collapse.getOrCreateInstance(parentSubmenu).show();
        parentSubmenu.previousElementSibling?.classList.add('has-active');
    }
    document.getElementById('page-title').textContent = TITLES[name];
    LOADERS[name]?.();
}

function goSection(name) {
    const link = document.querySelector(`.sidebar-sublink[onclick*="'${name}'"]`)
              || document.querySelector(`.sidebar-link[onclick*="'${name}'"]`);
    if (!link) return;
    showSection(name, link);
}

function goNewProduct(el) {
    document.querySelectorAll('.sidebar-link, .sidebar-sublink').forEach(l => l.classList.remove('active'));
    el.classList.add('active');
    const parentSubmenu = el.closest('.collapse');
    if (parentSubmenu) {
        bootstrap.Collapse.getOrCreateInstance(parentSubmenu).show();
        parentSubmenu.previousElementSibling?.classList.add('has-active');
    }
    SECTIONS.forEach(s => document.getElementById(`section-${s}`).classList.toggle('d-none', s !== 'inventario'));
    document.getElementById('page-title').textContent = TITLES['inventario'];
    loadInventario().then(() => openProductModal());
}

function goNewCategory(el) {
    document.querySelectorAll('.sidebar-link, .sidebar-sublink').forEach(l => l.classList.remove('active'));
    el.classList.add('active');
    const parentSubmenu = el.closest('.collapse');
    if (parentSubmenu) {
        bootstrap.Collapse.getOrCreateInstance(parentSubmenu).show();
        parentSubmenu.previousElementSibling?.classList.add('has-active');
    }
    SECTIONS.forEach(s => document.getElementById(`section-${s}`).classList.toggle('d-none', s !== 'categorias'));
    document.getElementById('page-title').textContent = TITLES['categorias'];
    loadCategorias().then(() => openCategoryModal());
}

let _pendingCatFilter = null;

function goProductsByCategory(categoryId, categoryName) {
    _pendingCatFilter = { id: String(categoryId), name: categoryName };
    goSection('inventario');
}
function updateAlertBadge(n) {
    const b = document.getElementById('badge-alerts');
    n > 0 ? (b.textContent = n, b.classList.remove('d-none')) : b.classList.add('d-none');
}
function isAdminOrSupervisor() { return currentUser?.role === 'ADMIN' || currentUser?.role === 'SUPERVISOR'; }

// ══════════════════════════════════════════════════════════
// DASHBOARD
// ══════════════════════════════════════════════════════════
async function loadDashboard() {
    try {
        const [summary, products, lowStock, sales] = await Promise.all([
            api.getSummary(), api.getProducts(), api.getLowStock(), api.getSales()
        ]);
        document.getElementById('stat-products').textContent = products.length;
        document.getElementById('stat-lowstock').textContent = lowStock.length;
        document.getElementById('stat-today').textContent = fmt(summary.totalToday);
        document.getElementById('stat-month').textContent = fmt(summary.totalThisMonth);
        updateAlertBadge(lowStock.length);

        document.getElementById('dash-lowstock').innerHTML = lowStock.length === 0
            ? emptyRow(3, '<i class="bi bi-check-circle text-success me-2"></i>Sin stock bajo')
            : lowStock.map(p => `<tr style="cursor:pointer" onclick="goSection('alertas')"><td>${p.name}</td><td><span class="badge-low">${p.stock} ${p.unit||''}</span></td><td>${p.minStock}</td></tr>`).join('');

        document.getElementById('dash-sales').innerHTML = sales.length === 0
            ? emptyRow(5, 'Sin ventas registradas')
            : sales.slice(0,5).map(s => `<tr style="cursor:pointer" onclick="goSection('ventas')"><td>#${s.id}</td><td>${s.customerName||'—'}</td><td>${s.sellerName||'—'}</td><td class="fw-bold text-success">${fmt(s.total)}</td><td class="text-muted small">${fmtDate(s.date)}</td></tr>`).join('');
    } catch(e) { toast('Error dashboard: '+e.message,'danger'); }
}

// ══════════════════════════════════════════════════════════
// INVENTARIO
// ══════════════════════════════════════════════════════════
async function loadInventario() {
    try {
        [allProducts, allCategories] = await Promise.all([api.getProducts(), api.getCategories()]);
        const f = document.getElementById('filter-category');
        f.innerHTML = '<option value="">Todas las categorías</option>' + allCategories.map(c=>`<option value="${c.id}">${c.name}</option>`).join('');
        const active = allProducts.filter(p => p.active).length;
        const lowStock = allProducts.filter(p => p.lowStock).length;
        document.getElementById('inv-stat-total').textContent   = allProducts.length;
        document.getElementById('inv-stat-active').textContent  = active;
        document.getElementById('inv-stat-low').textContent     = lowStock;
        document.getElementById('inv-stat-cats').textContent    = allCategories.length;
        // Ocultar controles de gestión para VENDEDOR
        const canManage = isAdminOrSupervisor();
        document.querySelector('#section-inventario .btn-primary')?.classList.toggle('d-none', !canManage);
        document.getElementById('subnav-nuevo-producto')?.classList.toggle('d-none', !canManage);

        if (_pendingCatFilter) {
            f.value = _pendingCatFilter.id;
            document.getElementById('search-product').value = '';
            document.getElementById('filter-status').value = '';
            document.getElementById('page-title').textContent = `Productos — ${_pendingCatFilter.name}`;
            _pendingCatFilter = null;
            filterProducts();
        } else {
            renderProducts(allProducts);
        }
    } catch(e) { toast('Error inventario: '+e.message,'danger'); }
}

function renderProducts(list) {
    document.getElementById('products-tbody').innerHTML = list.length === 0 ? emptyRow(9,'Sin productos')
        : list.map(p=>`<tr>
            <td><div class="fw-semibold">${p.name}</div><div class="text-muted small">${p.description||''}</div></td>
            <td>${p.categoryName||'—'}</td>
            <td><span class="text-muted small">${p.barcode||'—'}</span></td>
            <td>${fmt(p.price)}</td>
            <td><span class="badge bg-secondary">${p.taxRate||19}%</span></td>
            <td><span class="${p.lowStock?'badge-low':'badge-ok'}">${p.stock}</span></td>
            <td>${p.minStock}</td>
            <td><span class="badge ${p.active?'bg-success':'bg-secondary'}">${p.active?'Activo':'Inactivo'}</span></td>
            <td>${isAdminOrSupervisor()?`
              <button class="btn btn-xs btn-outline-primary me-1" onclick="openProductModal(${p.id})"><i class="bi bi-pencil"></i></button>
              <button class="btn btn-xs btn-outline-secondary me-1" onclick="openStockModal(${p.id},'${p.name.replace(/'/g,"\\'")}')"><i class="bi bi-arrow-left-right"></i></button>
              <button class="btn btn-xs btn-outline-danger" onclick="deactivateProduct(${p.id})"><i class="bi bi-trash"></i></button>`:'—'}
            </td></tr>`).join('');
}

function filterProducts() {
    const q   = document.getElementById('search-product').value.toLowerCase();
    const cat = document.getElementById('filter-category').value;
    const st  = document.getElementById('filter-status').value;
    renderProducts(allProducts.filter(p =>
        (p.name.toLowerCase().includes(q) || (p.barcode && p.barcode.toLowerCase().includes(q))) &&
        (!cat || String(p.categoryId) === cat) &&
        (!st  || (st === 'active' ? p.active : !p.active))
    ));
}

async function openProductModal(id=null) {
    document.getElementById('product-id').value = id||'';
    document.getElementById('modal-product-title').textContent = id ? 'Editar Producto' : 'Nuevo Producto';
    const cats = allCategories.length ? allCategories : await api.getCategories();
    document.getElementById('product-category').innerHTML = '<option value="">Sin categoría</option>'+cats.map(c=>`<option value="${c.id}">${c.name}</option>`).join('');
    if (id) {
        const p = allProducts.find(x=>x.id===id);
        if(p) { document.getElementById('product-name').value=p.name; document.getElementById('product-desc').value=p.description||''; document.getElementById('product-category').value=p.categoryId||''; document.getElementById('product-unit').value=p.unit||''; document.getElementById('product-barcode').value=p.barcode||''; document.getElementById('product-price').value=p.price; document.getElementById('product-taxrate').value=p.taxRate!=null?p.taxRate:19; document.getElementById('product-stock').value=p.stock; document.getElementById('product-minstock').value=p.minStock; }
    } else { ['product-name','product-desc','product-unit','product-price','product-barcode'].forEach(id=>document.getElementById(id).value=''); document.getElementById('product-stock').value='0'; document.getElementById('product-minstock').value='10'; document.getElementById('product-taxrate').value='19'; document.getElementById('product-category').value=''; }
    getModal('modalProduct').show();
}

async function saveProduct() {
    const id = document.getElementById('product-id').value;
    const data = { name:document.getElementById('product-name').value.trim(), description:document.getElementById('product-desc').value.trim(), categoryId:document.getElementById('product-category').value||null, unit:document.getElementById('product-unit').value.trim(), barcode:document.getElementById('product-barcode').value.trim()||null, price:parseFloat(document.getElementById('product-price').value), taxRate:parseFloat(document.getElementById('product-taxrate').value), stock:parseInt(document.getElementById('product-stock').value), minStock:parseInt(document.getElementById('product-minstock').value) };
    if (!data.name||isNaN(data.price)||isNaN(data.stock)||isNaN(data.minStock)) { toast('Completa los campos obligatorios','warning'); return; }
    try { id ? await api.updateProduct(id,data) : await api.createProduct(data); toast(id?'Producto actualizado':'Producto creado'); getModal('modalProduct').hide(); loadInventario(); }
    catch(e) { toast(e.message,'danger'); }
}

async function deactivateProduct(id) {
    if(!confirm('¿Desactivar este producto?')) return;
    try { await api.deleteProduct(id); toast('Producto desactivado','warning'); loadInventario(); } catch(e) { toast(e.message,'danger'); }
}

function openStockModal(id, name) {
    document.getElementById('stock-product-id').value=id; document.getElementById('stock-product-name').textContent=name;
    document.getElementById('stock-qty').value=''; document.getElementById('stock-reason').value=''; document.getElementById('stock-type').value='ENTRY';
    getModal('modalStock').show();
}

async function saveStockAdjust() {
    const id=document.getElementById('stock-product-id').value, qty=parseInt(document.getElementById('stock-qty').value);
    if(isNaN(qty)||qty<0) { toast('Cantidad inválida','warning'); return; }
    try {
        await api.adjustStock(id,{quantity:qty,type:document.getElementById('stock-type').value,reason:document.getElementById('stock-reason').value.trim()});
        toast('Stock actualizado');
        getModal('modalStock').hide();
        loadInventario();
        loadAlertas();
    }
    catch(e) { toast(e.message,'danger'); }
}

// ══════════════════════════════════════════════════════════
// VENTAS
// ══════════════════════════════════════════════════════════
let selectedSaleProduct = null;

async function loadVentas() {
    try {
        const [products, customers, cajas] = await Promise.all([api.getProducts(), api.getCustomers(), api.getCajasOpen()]);
        allProducts = products; allCustomers = customers;

        document.getElementById('sale-customer-select').innerHTML =
            '<option value="">— Cliente (opcional) —</option>' +
            customers.map(c=>`<option value="${c.id}">${c.name}</option>`).join('');

        // VENDEDOR: solo ve su propia caja abierta
        const cajasFiltered = isAdminOrSupervisor()
            ? cajas
            : cajas.filter(c => Number(c.sellerId) === Number(currentUser?.id));

        document.getElementById('sale-caja-select').innerHTML =
            '<option value="">— Caja (opcional) —</option>' +
            cajasFiltered.map(c=>`<option value="${c.id}">${c.name} - ${c.sellerName||'Sin vendedor'}</option>`).join('');

        // Auto-seleccionar la única caja del vendedor si tiene una
        if (!isAdminOrSupervisor() && cajasFiltered.length === 1) {
            document.getElementById('sale-caja-select').value = cajasFiltered[0].id;
        }

        // Aviso si el vendedor no tiene caja abierta
        const noBox = document.getElementById('no-caja-warning');
        if (noBox) noBox.classList.toggle('d-none', isAdminOrSupervisor() || cajasFiltered.length > 0);

        // Cargar historial: VENDEDOR ve solo ventas de su caja; ADMIN/SUPERVISOR ven todo
        let sales;
        if (!isAdminOrSupervisor() && cajasFiltered.length > 0) {
            // Busca ventas de todas las cajas del vendedor
            const cajaIds = cajasFiltered.map(c => c.id);
            const allSales = await Promise.all(cajaIds.map(id => api.getSalesByCaja(id)));
            sales = allSales.flat().sort((a, b) => new Date(b.date) - new Date(a.date));
        } else {
            sales = await api.getSales();
        }

        renderSales(sales);
        // Mostrar panel de efectivo si el método por defecto es EFECTIVO
        onPaymentMethodChange();
    } catch(e) { toast('Error ventas: '+e.message,'danger'); }
}

function filterSaleProducts() {
    const q = document.getElementById('sale-search').value.trim().toLowerCase();
    const dropdown = document.getElementById('sale-product-dropdown');
    if (!q) { dropdown.classList.add('d-none'); return; }
    const matches = allProducts.filter(p =>
        p.name.toLowerCase().includes(q) ||
        (p.barcode && p.barcode.toLowerCase().includes(q))
    ).slice(0, 8);
    if (matches.length === 0) {
        dropdown.innerHTML = '<div class="px-3 py-2 text-muted small">Sin resultados</div>';
    } else {
        dropdown.innerHTML = matches.map(p => `
            <div class="px-3 py-2 border-bottom d-flex align-items-center gap-2 sale-prod-item"
                 style="cursor:pointer" onmousedown="selectSaleProduct(${p.id})">
              <div class="flex-grow-1">
                <div class="fw-semibold small">${p.name}</div>
                <div class="text-muted" style="font-size:.75rem">
                  ${p.barcode?`<i class="bi bi-upc me-1"></i>${p.barcode} ·`:''} ${fmt(p.price)} · IVA ${p.taxRate||19}%
                </div>
              </div>
              <span class="${p.stock<=p.minStock?'badge-low':'badge-ok'}">${p.stock}</span>
            </div>`).join('');
    }
    dropdown.classList.remove('d-none');
}

function selectSaleProduct(id) {
    const p = allProducts.find(x => x.id === id);
    if (!p) return;
    selectedSaleProduct = p;
    document.getElementById('sale-search').value = '';
    document.getElementById('sale-product-dropdown').classList.add('d-none');
    const box = document.getElementById('sale-selected-product');
    document.getElementById('sel-prod-name').textContent = p.name + (p.barcode ? ` · ${p.barcode}` : '');
    document.getElementById('sel-prod-price').textContent = fmt(p.price);
    document.getElementById('sel-prod-tax').textContent = p.taxRate || 19;
    document.getElementById('sel-prod-stock').textContent = p.stock;
    box.classList.remove('d-none');
    document.getElementById('sale-qty').focus();
}

function clearSelectedProduct() {
    selectedSaleProduct = null;
    document.getElementById('sale-selected-product').classList.add('d-none');
    document.getElementById('sale-search').value = '';
}

// Cerrar dropdown al hacer click fuera
document.addEventListener('click', e => {
    if (!e.target.closest('#sale-search') && !e.target.closest('#sale-product-dropdown'))
        document.getElementById('sale-product-dropdown')?.classList.add('d-none');
});

function renderSales(sales) {
    document.getElementById('sales-tbody').innerHTML = sales.length===0 ? emptyRow(7,'Sin ventas')
        : sales.map(s=>`<tr>
            <td>#${s.id}</td><td>${s.customerName||'—'}</td><td>${s.cashRegisterName||'—'}</td><td>${s.sellerName||'—'}</td>
            <td class="fw-bold text-success">${fmt(s.total)}</td><td class="text-muted small">${fmtDate(s.date)}</td>
            <td><button class="btn btn-xs btn-outline-primary" onclick="viewSale(${s.id})"><i class="bi bi-eye"></i></button></td></tr>`).join('');
}

function addSaleItem() {
    const qty = parseInt(document.getElementById('sale-qty').value);
    if (!selectedSaleProduct) { toast('Busca y selecciona un producto primero','warning'); return; }
    if (qty < 1) { toast('La cantidad debe ser mayor a 0','warning'); return; }
    const p = selectedSaleProduct;
    const productId = p.id, name = p.name, price = parseFloat(p.price), stock = p.stock, taxRate = parseFloat(p.taxRate || 19);
    if (qty > stock) { toast(`Stock insuficiente. Disponible: ${stock}`,'danger'); return; }
    const ex = saleItems.find(i => i.productId === productId);
    if (ex) {
        if (ex.quantity + qty > stock) { toast(`Stock insuficiente`,'danger'); return; }
        ex.quantity += qty; ex.subtotal = ex.quantity * ex.price; ex.tax = ex.subtotal * ex.taxRate / 100;
    } else {
        const subtotal = price * qty;
        saleItems.push({ productId, name, price, taxRate, quantity: qty, subtotal, tax: subtotal * taxRate / 100 });
    }
    clearSelectedProduct();
    document.getElementById('sale-qty').value = 1;
    renderSaleItems();
}

function renderSaleItems() {
    document.getElementById('sale-items-tbody').innerHTML = saleItems.length===0 ? emptyRow(4,'Sin productos')
        : saleItems.map((i,idx)=>`<tr><td>${i.name}<span class="text-muted ms-1 small">IVA ${i.taxRate}%</span></td><td>${i.quantity}</td><td>${fmt(i.subtotal)}</td><td><button class="btn btn-xs btn-outline-danger" onclick="removeSaleItem(${idx})"><i class="bi bi-x"></i></button></td></tr>`).join('');
    const subtotal = saleItems.reduce((s,i)=>s+i.subtotal,0);
    const iva = saleItems.reduce((s,i)=>s+i.tax,0);
    document.getElementById('sale-subtotal').textContent = fmt(subtotal);
    document.getElementById('sale-iva').textContent = fmt(iva);
    document.getElementById('sale-total').textContent = fmt(subtotal+iva);
    calcChange();
}

function removeSaleItem(i) { saleItems.splice(i,1); renderSaleItems(); calcChange(); }

function calcChange() {
    const total = saleItems.reduce((s,i) => s + i.subtotal + i.tax, 0);
    const received = parseFloat(document.getElementById('cash-received').value) || 0;
    const change = received - total;
    const display = document.getElementById('change-display');
    const alert = document.getElementById('change-alert');
    if (received <= 0) { display.textContent = '—'; display.className = 'fw-bold fs-5 text-success px-2'; alert.classList.add('d-none'); return; }
    if (change < 0) {
        display.textContent = fmt(Math.abs(change)) + ' faltan';
        display.className = 'fw-bold fs-5 text-danger px-2';
        alert.classList.remove('d-none');
    } else {
        display.textContent = fmt(change);
        display.className = 'fw-bold fs-5 text-success px-2';
        alert.classList.add('d-none');
    }
}

function onPaymentMethodChange() {
    const method = document.getElementById('sale-payment').value;
    const panel = document.getElementById('cash-panel');
    if (method === 'EFECTIVO') { panel.classList.remove('d-none'); }
    else { panel.classList.add('d-none'); document.getElementById('cash-received').value = ''; }
    calcChange();
}

async function createSale() {
    if(saleItems.length===0) { toast('Agrega al menos un producto','warning'); return; }
    const customerId=document.getElementById('sale-customer-select').value||null;
    const cashRegisterId=document.getElementById('sale-caja-select').value||null;
    const paymentMethod=document.getElementById('sale-payment').value;
    if (paymentMethod === 'EFECTIVO') {
        const total = saleItems.reduce((s,i)=>s+i.subtotal+i.tax,0);
        const received = parseFloat(document.getElementById('cash-received').value)||0;
        if (received <= 0) { toast('Ingresa el efectivo recibido del cliente','warning'); document.getElementById('cash-received').focus(); return; }
        if (received < total) { toast('El efectivo recibido no cubre el total de la venta','danger'); document.getElementById('cash-received').focus(); return; }
    }
    try {
        await api.createSale({ customerId: customerId?parseInt(customerId):null, cashRegisterId: cashRegisterId?parseInt(cashRegisterId):null, paymentMethod, notes:document.getElementById('sale-notes').value.trim(), items:saleItems.map(i=>({productId:i.productId,quantity:i.quantity})) });
        toast('¡Venta registrada!'); saleItems=[]; renderSaleItems();
        document.getElementById('sale-notes').value=''; document.getElementById('cash-received').value=''; calcChange(); loadVentas();
    } catch(e) { toast(e.message,'danger'); }
}

async function viewSale(id) {
    try {
        currentSale = await api.getSale(id);
        const s = currentSale;
        document.getElementById('sale-detail-body').innerHTML = `
            <div class="row mb-3"><div class="col">
              <div class="fw-bold fs-6">Venta #${s.id}</div>
              <div class="text-muted small">${fmtDate(s.date)}</div>
            </div><div class="col text-end">
              ${s.customerName?`<div><i class="bi bi-person me-1"></i>${s.customerName}</div>`:''}
              ${s.cashRegisterName?`<div><i class="bi bi-cash-coin me-1"></i>${s.cashRegisterName}</div>`:''}
              ${s.sellerName?`<div><i class="bi bi-person-badge me-1"></i>${s.sellerName}</div>`:''}
            </div></div>
            <table class="table table-sm"><thead class="table-light"><tr><th>Producto</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead>
            <tbody>${s.items.map(i=>`<tr><td>${i.productName}</td><td>${i.quantity}</td><td>${fmt(i.unitPrice)}</td><td>${fmt(i.subtotal)}</td></tr>`).join('')}</tbody>
            <tfoot><tr class="fw-bold table-light"><td colspan="3" class="text-end">TOTAL</td><td class="text-success fs-5">${fmt(s.total)}</td></tr></tfoot></table>
            ${s.notes?`<p class="text-muted small mb-0"><i class="bi bi-chat-left-text me-1"></i>${s.notes}</p>`:''}`;
        getModal('modalSaleDetail').show();
    } catch(e) { toast(e.message,'danger'); }
}

// ── IMPRESIÓN ──────────────────────────────────────────────
// ── Helpers de documentos fiscales Colombia ──────────────────────────
function fmtNit(nit, dv) {
    if (!nit) return '';
    const n = String(nit).replace(/\D/g,'');
    const parts = [];
    let i = n.length;
    while (i > 0) { parts.unshift(n.slice(Math.max(0,i-3),i)); i-=3; }
    return parts.join('.') + (dv ? '-'+dv : '');
}

const REGIME_LABEL = {
    RESPONSABLE_IVA:   'Responsable de IVA — Régimen Ordinario',
    NO_RESPONSABLE_IVA:'No Responsable de IVA',
    GRAN_CONTRIBUYENTE:'Gran Contribuyente — Responsable de IVA',
    REGIMEN_SIMPLE:    'Régimen Simple de Tributación (SIMPLE)',
};
const PAY_LABEL_DOC = { EFECTIVO:'Efectivo', TARJETA:'Tarjeta Débito/Crédito', TRANSFERENCIA:'Transferencia Bancaria', CREDITO:'Crédito' };

function numToWords(n) {
    const num = Math.round(Number(n));
    if (!num) return 'CERO PESOS M/CTE';
    const U = ['','UN','DOS','TRES','CUATRO','CINCO','SEIS','SIETE','OCHO','NUEVE','DIEZ','ONCE','DOCE','TRECE','CATORCE','QUINCE','DIECISÉIS','DIECISIETE','DIECIOCHO','DIECINUEVE','VEINTE','VEINTIÚN','VEINTIDÓS','VEINTITRÉS','VEINTICUATRO','VEINTICINCO','VEINTISÉIS','VEINTISIETE','VEINTIOCHO','VEINTINUEVE'];
    const D = ['','','VEINTE','TREINTA','CUARENTA','CINCUENTA','SESENTA','SETENTA','OCHENTA','NOVENTA'];
    const H = ['','CIENTO','DOSCIENTOS','TRESCIENTOS','CUATROCIENTOS','QUINIENTOS','SEISCIENTOS','SETECIENTOS','OCHOCIENTOS','NOVECIENTOS'];
    function c3(x) {
        if (!x) return '';
        let r='';
        if (x===100) return 'CIEN';
        if (x>=100) { r=H[Math.floor(x/100)]+' '; x%=100; }
        if (x<30) r+=U[x];
        else { r+=D[Math.floor(x/10)]; if(x%10) r+=' Y '+U[x%10]; }
        return r.trim();
    }
    function c6(x) {
        if (!x) return '';
        const t=Math.floor(x/1000), r=x%1000;
        const ts = t===1?'MIL':c3(t)+' MIL';
        return (ts+(r?' '+c3(r):'')).trim();
    }
    let r='';
    if (num>=1000000) {
        const m=Math.floor(num/1000000), rem=num%1000000;
        r=(m===1?'UN MILLÓN':c3(m)+' MILLONES')+(rem?' '+c6(rem):'');
    } else { r=c6(num)||c3(num); }
    return r.trim()+' PESOS M/CTE';
}

function ivaGroupsOf(items) {
    const g={};
    (items||[]).forEach(i=>{
        const r=Number(i.taxRate||0);
        if(!g[r]) g[r]={base:0,tax:0};
        g[r].base+=Number(i.subtotal||0);
        g[r].tax+=Number(i.taxAmount||0);
    });
    return g;
}

// ── TIQUETE DE CAJA (80 mm, papel térmico) ───────────────────────────
function printTicket() {
    if(!currentSale) return;
    const s = currentSale;
    const e = empresaConfig || {};
    const nitFmt = e.nit ? `NIT ${fmtNit(e.nit, e.digitoVerificacion)}` : '';
    const regime = REGIME_LABEL[e.regime] || '';
    const ivaG   = ivaGroupsOf(s.items);
    const subtotal = Number(s.subtotal || s.total);
    const ivaTotal = Number(s.taxAmount || 0);
    const total    = Number(s.total);

    // Efectivo recibido (si el panel de caja está visible)
    const cashInput = document.getElementById('cash-received');
    const cashReceived = cashInput ? Number(cashInput.value||0) : 0;
    const change = cashReceived > total ? cashReceived - total : 0;

    const line  = (len=32) => '-'.repeat(len);
    const ctr   = (txt,len=32) => { const p=Math.max(0,Math.floor((len-txt.length)/2)); return ' '.repeat(p)+txt; };

    const ivaLines = Object.entries(ivaG).map(([r,g])=>
        `<tr><td>Base IVA ${r}%:</td><td style="text-align:right">${fmt(g.base)}</td></tr>`+
        (Number(r)>0?`<tr><td>IVA ${r}%:</td><td style="text-align:right">${fmt(g.tax)}</td></tr>`:'')
    ).join('');

    const cashSection = s.paymentMethod==='EFECTIVO' && cashReceived>=total ? `
    <tr><td>Efectivo recibido:</td><td style="text-align:right">${fmt(cashReceived)}</td></tr>
    <tr style="font-weight:bold"><td>Su cambio:</td><td style="text-align:right">${fmt(change)}</td></tr>` : '';

    const win = window.open('','_blank','width=360,height=800');
    win.document.write(`<!DOCTYPE html><html><head><meta charset="UTF-8">
    <title>Tiquete ${s.invoiceNumber||s.id}</title>
    <style>
      @page { size:80mm auto; margin:3mm; }
      *{box-sizing:border-box}
      body{font-family:'Courier New',Courier,monospace;font-size:11.5px;margin:0;padding:8px 10px;width:280px;color:#000;line-height:1.4}
      .c{text-align:center}.b{font-weight:bold}
      .empresa{font-size:14px;font-weight:bold;text-align:center;margin:0 0 1px}
      .sep-d{border:none;border-top:1px dashed #000;margin:5px 0}
      .sep-s{border:none;border-top:1px solid #000;margin:5px 0}
      table{width:100%;border-collapse:collapse;font-size:11px}
      td{padding:1px 0;vertical-align:top}
      .items thead td{font-weight:bold;border-bottom:1px solid #000}
      .items td.r{text-align:right}
      .total-row td{font-size:13px;font-weight:bold;padding-top:4px}
      .aviso{font-size:10px;text-align:center;font-style:italic;border:1px solid #000;padding:3px;margin:6px 0}
      @media print{body{padding:4px 6px}}
    </style></head><body>

    <p class="empresa">${e.razonSocial||'MI EMPRESA'}</p>
    ${e.nombreComercial&&e.nombreComercial!==e.razonSocial?`<p class="c" style="font-size:11px">${e.nombreComercial}</p>`:''}
    ${nitFmt?`<p class="c">${nitFmt}</p>`:''}
    ${e.address?`<p class="c" style="font-size:10px">${e.address}${e.city?', '+e.city:''}${e.department?', '+e.department:''}</p>`:''}
    ${e.phone?`<p class="c" style="font-size:10px">Tel: ${e.phone}</p>`:''}
    ${regime?`<p class="c" style="font-size:10px">${regime}</p>`:''}

    <hr class="sep-s">
    <p class="c b" style="font-size:13px">TIQUETE DE CAJA</p>
    ${e.dianResolutionNumber?`
    <p class="c" style="font-size:10px">Res. DIAN No. <b>${e.dianResolutionNumber}</b>${e.dianResolutionDate?' del '+e.dianResolutionDate:''}</p>
    <p class="c" style="font-size:10px">Rango: <b>${e.dianRangeFrom||'?'}</b> al <b>${e.dianRangeTo||'?'}</b></p>`:''}
    <hr class="sep-d">

    <table>
      <tr><td><b>No.:</b></td><td style="text-align:right"><b>${s.invoiceNumber||('#'+s.id)}</b></td></tr>
      <tr><td>Fecha:</td><td style="text-align:right">${fmtDate(s.date)}</td></tr>
      <tr><td>Forma de pago:</td><td style="text-align:right">${PAY_LABEL_DOC[s.paymentMethod]||s.paymentMethod||'EFECTIVO'}</td></tr>
      ${s.customerName?`<tr><td>Cliente:</td><td style="text-align:right">${s.customerName}</td></tr>`:''}
      ${s.customerDocument?`<tr><td>Doc./NIT:</td><td style="text-align:right">${s.customerDocument}</td></tr>`:''}
      <tr><td>Vendedor:</td><td style="text-align:right">${s.sellerName||'—'}</td></tr>
      ${s.cashRegisterName?`<tr><td>Caja:</td><td style="text-align:right">${s.cashRegisterName}</td></tr>`:''}
    </table>
    <hr class="sep-d">

    <table class="items">
      <thead>
        <tr>
          <td style="width:48%">Descripción</td>
          <td class="r" style="width:12%">Cant</td>
          <td class="r" style="width:18%">V/Unit</td>
          <td class="r" style="width:10%">IVA</td>
          <td class="r" style="width:12%">Total</td>
        </tr>
      </thead>
      <tbody>
        ${(s.items||[]).map(i=>{
            const disc = Number(i.discountAmount||0);
            return `<tr>
              <td>${i.productName}</td>
              <td class="r">${i.quantity}</td>
              <td class="r">${fmt(i.unitPrice)}</td>
              <td class="r">${Number(i.taxRate||0)}%</td>
              <td class="r">${fmt(i.subtotal)}</td>
            </tr>${disc>0?`<tr><td colspan="4" style="font-size:10px;padding-left:8px">Descuento:</td><td class="r" style="font-size:10px">-${fmt(disc)}</td></tr>`:''}`
        }).join('')}
      </tbody>
    </table>
    <hr class="sep-d">

    <table>
      <tr><td>Base gravable:</td><td style="text-align:right">${fmt(subtotal)}</td></tr>
      ${ivaLines}
      <hr class="sep-d">
      <tr class="total-row"><td>TOTAL A PAGAR:</td><td style="text-align:right">${fmt(total)}</td></tr>
      ${cashSection}
    </table>

    ${s.notes?`<hr class="sep-d"><p style="font-size:10px"><b>Nota:</b> ${s.notes}</p>`:''}
    <hr class="sep-s">
    <p class="c" style="font-size:11px">${e.ticketFooter||'¡Gracias por su compra!'}</p>
    <div class="aviso">Este tiquete NO es una Factura de Venta.<br>No tiene validez fiscal.</div>
    <p class="c" style="font-size:9px">Conservar para reclamaciones</p>

    <script>window.onload=()=>{window.print();}<\/script>
    </body></html>`);
    win.document.close();
}

// ── FACTURA DE VENTA (Carta/A4, estándar DIAN Colombia) ──────────────
function printFactura() {
    if(!currentSale) return;
    const s = currentSale;
    const e = empresaConfig || {};
    const nitFmt  = e.nit ? `NIT ${fmtNit(e.nit,e.digitoVerificacion)}` : '';
    const regime  = REGIME_LABEL[e.regime] || '';
    const ivaG    = ivaGroupsOf(s.items);
    const subtotal = Number(s.subtotal || s.total);
    const ivaTotal = Number(s.taxAmount || 0);
    const total    = Number(s.total);
    const totalWords = numToWords(total);
    const hoy     = new Date(s.date).toLocaleDateString('es-CO',{day:'2-digit',month:'long',year:'numeric'});

    // IVA discriminado por tarifa
    const ivaRows = Object.entries(ivaG).sort(([a],[b])=>a-b).map(([r,g])=>`
      <tr>
        <td class="tr">Base gravable IVA ${r}%:</td>
        <td class="tr">${fmt(g.base)}</td>
        <td class="tr">${r}%</td>
        <td class="tr money">${fmt(g.tax)}</td>
      </tr>`).join('');

    const totalDesc = (s.items||[]).reduce((a,i)=>a+Number(i.discountAmount||0),0);
    const win = window.open('','_blank','width=900,height=900');
    win.document.write(`<!DOCTYPE html><html><head><meta charset="UTF-8">
    <title>Factura ${s.invoiceNumber||s.id}</title>
    <style>
      @page { size:letter; margin:12mm 15mm 15mm 15mm; }
      *{box-sizing:border-box}
      body{font-family:'Segoe UI',Arial,sans-serif;margin:0;color:#1a1a1a;font-size:12px;line-height:1.4}
      /* ── Header ── */
      .hdr{display:flex;justify-content:space-between;align-items:stretch;gap:0;margin-bottom:0}
      .hdr-co{flex:1;padding:14px 16px;border:2px solid #1a2540;border-right:none;border-radius:4px 0 0 4px}
      .hdr-co h1{margin:0 0 3px;font-size:18px;color:#1a2540;letter-spacing:-.3px}
      .hdr-co p{margin:1px 0;font-size:11px;color:#444}
      .hdr-fv{background:#1a2540;color:#fff;padding:14px 16px;border-radius:0 4px 4px 0;text-align:center;min-width:195px;display:flex;flex-direction:column;justify-content:center}
      .hdr-fv h2{margin:0 0 6px;font-size:17px;letter-spacing:.5px}
      .hdr-fv .no{font-size:22px;font-weight:700;letter-spacing:1px}
      .hdr-fv p{margin:2px 0;font-size:11px;opacity:.9}
      /* ── DIAN bar ── */
      .dian{background:#eef2fb;border-left:4px solid #1a2540;padding:6px 12px;font-size:11px;color:#333;margin:8px 0}
      /* ── Cajas de datos ── */
      .boxes{display:flex;gap:10px;margin:10px 0}
      .box{flex:1;border:1px solid #d0d7e5;border-radius:4px;padding:10px 12px}
      .box h4{margin:0 0 6px;font-size:10px;color:#1a2540;text-transform:uppercase;letter-spacing:.6px;font-weight:700;border-bottom:1px solid #d0d7e5;padding-bottom:4px}
      .box p{margin:2px 0;font-size:11.5px}
      /* ── Tabla ítems ── */
      table.items{width:100%;border-collapse:collapse;margin:8px 0;font-size:11.5px}
      table.items thead tr{background:#1a2540;color:#fff}
      table.items thead td{padding:7px 8px;font-weight:600}
      table.items tbody tr:nth-child(even){background:#f7f9fc}
      table.items tbody td{padding:6px 8px;border-bottom:1px solid #e8edf5}
      table.items tfoot td{padding:6px 8px;border-top:2px solid #1a2540;font-weight:600;background:#f0f3f9}
      .tr{text-align:right}.tc{text-align:center}
      /* ── Totales ── */
      .totales-wrap{display:flex;justify-content:flex-end;margin-top:6px}
      .totales{width:320px;border:1px solid #d0d7e5;border-radius:4px;overflow:hidden;font-size:12px}
      .totales table{width:100%;border-collapse:collapse}
      .totales td{padding:5px 12px}
      .totales tr:nth-child(odd){background:#f7f9fc}
      .totales .iva-head td{background:#fff3cd;font-weight:600;font-size:11px;color:#7d5c00}
      .totales .gran-total td{background:#1a2540;color:#fff;font-size:15px;font-weight:700;padding:9px 12px}
      /* ── Letras ── */
      .letras{background:#f0f3f9;border:1px solid #d0d7e5;border-radius:4px;padding:8px 12px;margin:10px 0;font-size:11.5px}
      .letras b{color:#1a2540}
      /* ── Firmas ── */
      .firmas{display:flex;gap:30px;margin-top:28px}
      .firma{flex:1;border-top:1px solid #333;padding-top:6px;font-size:11px;text-align:center}
      /* ── Footer ── */
      .footer{margin-top:16px;padding-top:8px;border-top:2px solid #1a2540;display:flex;justify-content:space-between;font-size:10px;color:#666}
      .badge-orig{background:#1a2540;color:#fff;padding:2px 8px;border-radius:3px;font-size:10px;font-weight:700}
      @media print{body{font-size:11.5px}}
    </style></head><body>

    <div class="hdr">
      <div class="hdr-co">
        <h1>${e.razonSocial||'MI EMPRESA'}</h1>
        ${e.nombreComercial&&e.nombreComercial!==e.razonSocial?`<p><i>${e.nombreComercial}</i></p>`:''}
        ${nitFmt?`<p><b>${nitFmt}</b></p>`:''}
        ${e.address?`<p>${e.address}${e.city?', '+e.city:''}${e.department?', '+e.department:''}</p>`:''}
        ${e.phone?`<p>Tel: ${e.phone}${e.email?' &nbsp;|&nbsp; '+e.email:''}</p>`:''}
        ${regime?`<p style="color:#1a2540;font-weight:600">${regime}</p>`:''}
      </div>
      <div class="hdr-fv">
        <h2>FACTURA DE VENTA</h2>
        <div class="no">${s.invoiceNumber||('FV-'+String(s.id).padStart(5,'0'))}</div>
        <p>Fecha: <b>${hoy}</b></p>
        <p>Pago: ${PAY_LABEL_DOC[s.paymentMethod]||s.paymentMethod||'EFECTIVO'}</p>
      </div>
    </div>

    ${e.dianResolutionNumber?`
    <div class="dian">
      ⚖️ <b>Autorización DIAN:</b> Resolución No. <b>${e.dianResolutionNumber}</b>
      ${e.dianResolutionDate?' del <b>'+new Date(e.dianResolutionDate).toLocaleDateString('es-CO',{day:'2-digit',month:'long',year:'numeric'})+'</b>':''}
      &nbsp;—&nbsp; Rango autorizado del No. <b>${e.dianRangeFrom||'N/A'}</b> al No. <b>${e.dianRangeTo||'N/A'}</b>
    </div>`:''}

    <div class="boxes">
      <div class="box">
        <h4>Datos del Adquiriente</h4>
        <p><b>${s.customerName||'CONSUMIDOR FINAL'}</b></p>
        ${s.customerDocument?`<p>NIT / Doc.: ${s.customerDocument}</p>`:''}
        ${s.customerAddress?`<p>Dirección: ${s.customerAddress}</p>`:''}
        <p>Forma de pago: <b>${PAY_LABEL_DOC[s.paymentMethod]||s.paymentMethod||'EFECTIVO'}</b></p>
      </div>
      <div class="box">
        <h4>Punto de Venta / Vendedor</h4>
        ${s.sellerName?`<p>Vendedor: <b>${s.sellerName}</b></p>`:''}
        ${s.cashRegisterName?`<p>Caja: ${s.cashRegisterName}</p>`:''}
        <p>Fecha expedición: ${hoy}</p>
        <p>Condición: <b>Contado</b></p>
      </div>
    </div>

    <table class="items">
      <thead>
        <tr>
          <td style="width:4%" class="tc">#</td>
          <td style="width:34%">Descripción del Producto / Servicio</td>
          <td style="width:7%" class="tc">Und.</td>
          <td style="width:6%" class="tc">Cant.</td>
          <td style="width:12%" class="tr">V/r Unitario</td>
          <td style="width:8%" class="tr">% Dto.</td>
          <td style="width:11%" class="tr">V/r Descto.</td>
          <td style="width:6%" class="tc">% IVA</td>
          <td style="width:12%" class="tr">Total</td>
        </tr>
      </thead>
      <tbody>
        ${(s.items||[]).map((i,idx)=>{
            const disc  = Number(i.discountAmount||0);
            const discP = Number(i.discountPercent||0);
            return `<tr>
              <td class="tc">${idx+1}</td>
              <td>${i.productName}</td>
              <td class="tc">Und</td>
              <td class="tc">${i.quantity}</td>
              <td class="tr">${fmt(i.unitPrice)}</td>
              <td class="tr">${discP>0?discP+'%':'—'}</td>
              <td class="tr">${disc>0?fmt(disc):'—'}</td>
              <td class="tc">${Number(i.taxRate||0)}%</td>
              <td class="tr"><b>${fmt(i.subtotal)}</b></td>
            </tr>`;
        }).join('')}
      </tbody>
      <tfoot>
        <tr>
          <td colspan="6"></td>
          <td class="tr" colspan="2">Subtotal artículos:</td>
          <td class="tr">${fmt(subtotal)}</td>
        </tr>
      </tfoot>
    </table>

    <div class="totales-wrap">
      <div class="totales">
        <table>
          <tr class="iva-head"><td colspan="4"><b>DISCRIMINACIÓN DEL IVA (Art. 617 E.T.)</b></td></tr>
          <tr style="font-size:10.5px;color:#555;font-weight:600">
            <td>Concepto</td><td class="tr">Base Gravable</td><td class="tr">Tarifa</td><td class="tr">IVA</td>
          </tr>
          ${ivaRows}
          <tr><td colspan="2">Total descuentos:</td><td></td><td class="tr">${totalDesc>0?'-'+fmt(totalDesc):'$0.00'}</td></tr>
          <tr style="font-weight:600"><td colspan="2">Total base gravable:</td><td></td><td class="tr">${fmt(subtotal)}</td></tr>
          <tr style="font-weight:600"><td colspan="2">Total IVA:</td><td></td><td class="tr">${fmt(ivaTotal)}</td></tr>
          <tr class="gran-total"><td colspan="2">TOTAL A PAGAR:</td><td></td><td class="tr">${fmt(total)}</td></tr>
        </table>
      </div>
    </div>

    <div class="letras"><b>SON:</b> ${totalWords}</div>

    ${s.notes?`<p style="font-size:11px;color:#555;margin:6px 0"><b>Observaciones:</b> ${s.notes}</p>`:''}

    <div class="firmas">
      <div class="firma">
        <p style="margin:0">______________________________</p>
        <p style="margin:2px 0"><b>${e.razonSocial||'Empresa'}</b></p>
        <p style="margin:0;color:#666">${nitFmt}</p>
        <p style="margin:0;color:#666">Firma autorizada / Vendedor</p>
      </div>
      <div class="firma">
        <p style="margin:0">______________________________</p>
        <p style="margin:2px 0"><b>${s.customerName||'Consumidor Final'}</b></p>
        ${s.customerDocument?`<p style="margin:0;color:#666">Doc.: ${s.customerDocument}</p>`:''}
        <p style="margin:0;color:#666">Firma del Adquiriente / Recibí a satisfacción</p>
      </div>
    </div>

    <div class="footer">
      <span><span class="badge-orig">ORIGINAL DEL ADQUIRIENTE</span> &nbsp; Conservar para efectos fiscales (Art. 632 E.T.)</span>
      <span>Impreso el ${new Date().toLocaleDateString('es-CO',{day:'2-digit',month:'2-digit',year:'numeric'})} &nbsp; ${regime}</span>
    </div>

    <script>window.onload=()=>{window.print();}<\/script>
    </body></html>`);
    win.document.close();
}

// ══════════════════════════════════════════════════════════
// CLIENTES
// ══════════════════════════════════════════════════════════
async function loadClientes() {
    try { allCustomers = await api.getCustomers(); renderCustomers(allCustomers); } catch(e) { toast('Error clientes: '+e.message,'danger'); }
}

async function searchCustomers() {
    const q = document.getElementById('search-customer').value.trim();
    try { renderCustomers(q ? await api.searchCustomers(q) : allCustomers); } catch(e) {}
}

function renderCustomers(list) {
    document.getElementById('customers-tbody').innerHTML = list.length===0 ? emptyRow(6,'Sin clientes')
        : list.map(c=>`<tr>
            <td class="fw-semibold">${c.name}</td><td>${c.document||'—'}</td><td>${c.phone||'—'}</td>
            <td>${c.email||'—'}</td><td>${c.address||'—'}</td>
            <td>
              <button class="btn btn-xs btn-outline-primary me-1" onclick="openCustomerModal(${c.id})"><i class="bi bi-pencil"></i></button>
              <button class="btn btn-xs btn-outline-danger" onclick="deleteCustomer(${c.id})"><i class="bi bi-trash"></i></button>
            </td></tr>`).join('');
}

function openCustomerModal(id=null) {
    document.getElementById('customer-id').value=id||'';
    document.getElementById('modal-customer-title').textContent=id?'Editar Cliente':'Nuevo Cliente';
    const c=id?allCustomers.find(x=>x.id===id):null;
    document.getElementById('customer-name').value=c?.name||''; document.getElementById('customer-doc').value=c?.document||'';
    document.getElementById('customer-phone').value=c?.phone||''; document.getElementById('customer-email').value=c?.email||'';
    document.getElementById('customer-address').value=c?.address||'';
    getModal('modalCustomer').show();
}

async function saveCustomer() {
    const id=document.getElementById('customer-id').value;
    const data={name:document.getElementById('customer-name').value.trim(), document:document.getElementById('customer-doc').value.trim(), phone:document.getElementById('customer-phone').value.trim(), email:document.getElementById('customer-email').value.trim(), address:document.getElementById('customer-address').value.trim()};
    if(!data.name){ toast('El nombre es obligatorio','warning'); return; }
    try { id?await api.updateCustomer(id,data):await api.createCustomer(data); toast(id?'Cliente actualizado':'Cliente creado'); getModal('modalCustomer').hide(); loadClientes(); }
    catch(e){ toast(e.message,'danger'); }
}

async function deleteCustomer(id) {
    if(!confirm('¿Eliminar este cliente?')) return;
    try { await api.deleteCustomer(id); toast('Cliente eliminado','warning'); loadClientes(); } catch(e){ toast(e.message,'danger'); }
}

// ══════════════════════════════════════════════════════════
// CAJAS
// ══════════════════════════════════════════════════════════
let allCajas = [];

function showCajasTab(tab, el) {
    ['resumen','productividad','historial'].forEach(t => document.getElementById(`cajas-tab-${t}`).classList.toggle('d-none', t !== tab));
    document.querySelectorAll('#cajas-tabs .nav-link').forEach(l => l.classList.remove('active'));
    el.classList.add('active');
    if(tab === 'productividad') loadProductividad();
}

async function loadCajas() {
    try {
        const fetchUsers = isAdminOrSupervisor() ? api.getUsers().catch(()=>[]) : Promise.resolve([]);
        const [cajas, users] = await Promise.all([api.getCajas(), fetchUsers]);
        allCajas = cajas;
        if(users.length) allUsers = users;

        const open = cajas.filter(c=>c.status==='OPEN');
        const openRow = document.getElementById('cajas-open-row');
        openRow.innerHTML = open.length===0 ? ''
            : open.map(c=>`<div class="col-md-4"><div class="card border-0 shadow-sm border-start border-success border-4">
                <div class="card-body"><div class="d-flex justify-content-between"><div>
                  <h6 class="fw-bold mb-1"><i class="bi bi-cash-coin text-success me-2"></i>${c.name}</h6>
                  <div class="small text-muted">${c.sellerName||'Sin vendedor'}</div>
                  <div class="small text-muted">Apertura: ${fmtDate(c.openedAt)}</div>
                </div><div class="text-end">
                  <div class="small text-muted">Ventas</div>
                  <div class="fw-bold text-success">${fmt(c.totalSales)}</div>
                  <div class="small text-muted mt-1">Balance</div>
                  <div class="fw-bold">${fmt(c.balance)}</div>
                </div></div>
                ${isAdminOrSupervisor()?`<button class="btn btn-sm btn-outline-danger mt-2 w-100" onclick="closeCaja(${c.id})"><i class="bi bi-lock me-1"></i>Cerrar Caja</button>`:''}
              </div></div></div>`).join('');
        document.getElementById('cajas-empty-msg').style.display = open.length===0 ? '' : 'none';

        document.getElementById('cajas-tbody').innerHTML = cajas.length===0 ? emptyRow(9,'Sin cajas')
            : cajas.map(c=>`<tr>
                <td class="fw-semibold">${c.name}</td><td>${c.sellerName||'—'}</td>
                <td><span class="badge ${c.status==='OPEN'?'bg-success':'bg-secondary'}">${c.status==='OPEN'?'Abierta':'Cerrada'}</span></td>
                <td class="small">${c.openedAt?fmtDate(c.openedAt):'—'}</td>
                <td class="small">${c.closedAt?fmtDate(c.closedAt):'—'}</td>
                <td>${fmt(c.initialAmount)}</td>
                <td class="text-success fw-semibold">${fmt(c.totalSales)}</td>
                <td class="fw-bold">${fmt(c.balance)}</td>
                <td>${(isAdminOrSupervisor()&&c.status==='OPEN')?`<button class="btn btn-xs btn-outline-danger" onclick="closeCaja(${c.id})"><i class="bi bi-lock"></i></button>`:'—'}</td></tr>`).join('');
    } catch(e){ toast('Error cajas: '+e.message,'danger'); }
}

function loadProductividad() {
    const from = document.getElementById('prod-from').value;
    const to   = document.getElementById('prod-to').value;

    let cajas = allCajas.length ? allCajas : [];

    if (from || to) {
        const fromMs = from ? new Date(from + 'T00:00:00').getTime() : null;
        const toMs   = to   ? new Date(to   + 'T23:59:59').getTime() : null;
        cajas = cajas.filter(c => {
            if (!c.openedAt) return false;
            const d = Array.isArray(c.openedAt)
                ? new Date(c.openedAt[0], c.openedAt[1]-1, c.openedAt[2], c.openedAt[3]||0, c.openedAt[4]||0)
                : new Date(c.openedAt);
            const ms = d.getTime();
            if (fromMs && ms < fromMs) return false;
            if (toMs   && ms > toMs)   return false;
            return true;
        });
    }

    const totalVentas  = cajas.reduce((s,c)=>s+Number(c.totalSales||0),0);
    const totalBalance = cajas.reduce((s,c)=>s+Number(c.balance||0),0);
    const abiertas     = cajas.filter(c=>c.status==='OPEN').length;
    const rangoLabel   = (from||to) ? ` <span class="badge bg-primary-subtle text-primary fw-normal ms-2">${from||'…'} → ${to||'…'}</span>` : '';

    document.getElementById('prod-stats-row').innerHTML = `
        <div class="col-md-4"><div class="card border-0 shadow-sm text-center py-3">
          <div class="text-muted small">Total Ventas (cajas filtradas)${rangoLabel}</div>
          <div class="fw-bold fs-4 text-success">${fmt(totalVentas)}</div>
        </div></div>
        <div class="col-md-4"><div class="card border-0 shadow-sm text-center py-3">
          <div class="text-muted small">Balance General</div>
          <div class="fw-bold fs-4">${fmt(totalBalance)}</div>
        </div></div>
        <div class="col-md-4"><div class="card border-0 shadow-sm text-center py-3">
          <div class="text-muted small">Cajas Abiertas</div>
          <div class="fw-bold fs-4 text-primary">${abiertas}</div>
        </div></div>`;
    document.getElementById('prod-tbody').innerHTML = cajas.length===0 ? emptyRow(9,'Sin cajas en el rango seleccionado')
        : cajas.map(c=>`<tr>
            <td class="fw-semibold">${c.name}</td>
            <td>${c.sellerName||'—'}</td>
            <td><span class="badge ${c.status==='OPEN'?'bg-success':'bg-secondary'}">${c.status==='OPEN'?'Abierta':'Cerrada'}</span></td>
            <td class="text-muted small">${c.openedAt?fmtDate(c.openedAt):'—'}</td>
            <td class="text-muted small">${c.closedAt?fmtDate(c.closedAt):'—'}</td>
            <td class="text-success fw-bold">${fmt(c.totalSales)}</td>
            <td class="text-danger">${fmt(c.totalExpenses)}</td>
            <td class="fw-bold">${fmt(c.balance)}</td>
            <td>${fmt(c.initialAmount)}</td></tr>`).join('');
}

async function openCajaModal() {
    document.getElementById('caja-name').value=''; document.getElementById('caja-initial').value='0'; document.getElementById('caja-notes').value='';
    const sellerRow = document.getElementById('caja-seller-row');
    if(isAdminOrSupervisor()) {
        const users = allUsers.length ? allUsers : await api.getUsers().catch(()=>[]);
        document.getElementById('caja-seller').innerHTML='<option value="">— Sin asignar —</option>'+users.map(u=>`<option value="${u.id}">${u.fullName} (${u.role})</option>`).join('');
        if(sellerRow) sellerRow.classList.remove('d-none');
    } else {
        document.getElementById('caja-seller').innerHTML=`<option value="${currentUser.id}" selected>${currentUser.fullName}</option>`;
        if(sellerRow) sellerRow.classList.add('d-none');
    }
    getModal('modalCaja').show();
}

async function saveCaja() {
    const name=document.getElementById('caja-name').value.trim();
    const sellerId=document.getElementById('caja-seller').value||null;
    const initialAmount=parseFloat(document.getElementById('caja-initial').value)||0;
    if(!name){ toast('El nombre de la caja es obligatorio','warning'); return; }
    try { await api.openCaja({name,sellerId:sellerId?parseInt(sellerId):null,initialAmount,notes:document.getElementById('caja-notes').value.trim()}); toast('Caja abierta'); getModal('modalCaja').hide(); loadCajas(); }
    catch(e){ toast(e.message,'danger'); }
}

async function closeCaja(id) {
    if(!confirm('¿Cerrar esta caja?')) return;
    try { await api.closeCaja(id,{}); toast('Caja cerrada','warning'); loadCajas(); } catch(e){ toast(e.message,'danger'); }
}

// ══════════════════════════════════════════════════════════
// CATEGORÍAS
// ══════════════════════════════════════════════════════════
async function loadCategorias() {
    try {
        [allCategories, allProducts] = await Promise.all([api.getCategories(), api.getProducts()]);
        const sinCat = allProducts.filter(p => !p.categoryId).length;
        document.getElementById('cat-stat-total').textContent  = allCategories.length;
        document.getElementById('cat-stat-sincat').textContent = sinCat;
        renderCategories(allCategories);
    } catch(e){ toast('Error categorías: '+e.message,'danger'); }
}

function renderCategories(list) {
    const counts = {};
    allProducts.forEach(p => { if (p.categoryId) counts[p.categoryId] = (counts[p.categoryId]||0)+1; });
    document.getElementById('categories-tbody').innerHTML = list.length===0 ? emptyRow(5,'Sin categorías')
        : list.map(c=>`<tr>
            <td class="fw-semibold">${c.name}</td>
            <td class="text-muted">${c.description||'—'}</td>
            <td>
              <span class="badge bg-primary bg-opacity-75 me-2">${counts[c.id]||0}</span>
              ${counts[c.id] ? `<a href="#" class="small text-primary" onclick="goProductsByCategory(${c.id},'${c.name.replace(/'/g,"\\'")}');return false"><i class="bi bi-arrow-right"></i> Ver</a>` : ''}
            </td>
            <td>
              <button class="btn btn-xs btn-outline-primary me-1" onclick="openCategoryModal(${c.id})"><i class="bi bi-pencil"></i></button>
              <button class="btn btn-xs btn-outline-danger" onclick="deleteCategory(${c.id})"><i class="bi bi-trash"></i></button>
            </td></tr>`).join('');
}

function filterCategories() {
    const q = document.getElementById('search-category').value.toLowerCase();
    renderCategories(allCategories.filter(c => c.name.toLowerCase().includes(q) || (c.description||'').toLowerCase().includes(q)));
}

function openCategoryModal(id=null) {
    document.getElementById('cat-id').value=id||'';
    document.getElementById('modal-cat-title').textContent=id?'Editar Categoría':'Nueva Categoría';
    const c=id?allCategories.find(x=>x.id===id):null;
    document.getElementById('cat-name').value=c?.name||''; document.getElementById('cat-desc').value=c?.description||'';
    getModal('modalCategory').show();
}

async function saveCategory() {
    const id=document.getElementById('cat-id').value;
    const data={name:document.getElementById('cat-name').value.trim(), description:document.getElementById('cat-desc').value.trim()};
    if(!data.name){ toast('El nombre es obligatorio','warning'); return; }
    try { id?await api.updateCategory(id,data):await api.createCategory(data); toast(id?'Categoría actualizada':'Categoría creada'); getModal('modalCategory').hide(); loadCategorias(); }
    catch(e){ toast(e.message,'danger'); }
}

async function deleteCategory(id) {
    if(!confirm('¿Eliminar esta categoría?')) return;
    try { await api.deleteCategory(id); toast('Categoría eliminada','warning'); loadCategorias(); } catch(e){ toast(e.message,'danger'); }
}

// ══════════════════════════════════════════════════════════
// ALERTAS — lista dinámica: muestra productos con stock <= minStock
// ══════════════════════════════════════════════════════════
async function loadAlertas() {
    try {
        const products = await api.getLowStock();
        updateAlertBadge(products.length);
        document.getElementById('alerts-tbody').innerHTML = products.length === 0
            ? emptyRow(5,'<i class="bi bi-check-circle text-success me-2"></i>Todos los productos tienen stock suficiente')
            : products.map(p=>`<tr>
                <td><div class="fw-semibold">${p.name}</div><div class="text-muted small">${p.description||''}</div></td>
                <td>${p.categoryName||'—'}</td>
                <td><span class="badge bg-danger fs-6">${p.stock}${p.unit?' '+p.unit:''}</span></td>
                <td>${p.minStock}${p.unit?' '+p.unit:''}</td>
                <td><button class="btn btn-xs btn-outline-primary" onclick="openStockModal(${p.id},'${p.name.replace(/'/g,"\\'")}')"><i class="bi bi-arrow-left-right me-1"></i>Ajustar Stock</button></td>
            </tr>`).join('');
    } catch(e){ toast('Error alertas: '+e.message,'danger'); }
}

// ══════════════════════════════════════════════════════════
// USUARIOS (solo ADMIN)
// ══════════════════════════════════════════════════════════
function showUsuariosTab(tab, el) {
    ['lista','historial'].forEach(t => document.getElementById(`usuarios-tab-${t}`).classList.toggle('d-none', t !== tab));
    document.querySelectorAll('#usuarios-tabs .nav-link').forEach(l => l.classList.remove('active'));
    el.classList.add('active');
    if (tab === 'historial') loadPasswordLogs();
}

async function loadPasswordLogs() {
    const tbody = document.getElementById('password-logs-tbody');
    tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-3"><span class="spinner-border spinner-border-sm me-2"></span>Cargando...</td></tr>';
    try {
        const logs = await fetch('/api/auth/password-reset-logs').then(r => r.json());
        if (!logs.length) { tbody.innerHTML = '<tr><td colspan="5" class="text-center text-muted py-4"><i class="bi bi-inbox me-2"></i>Sin registros aún</td></tr>'; return; }
        const fmtDate = d => new Date(d).toLocaleString('es-CO', { dateStyle:'short', timeStyle:'short' });
        const badgeClass = a => a === 'REQUEST' ? 'bg-warning text-dark' : 'bg-success';
        tbody.innerHTML = logs.map(l => `
            <tr>
              <td class="small">${fmtDate(l.createdAt)}</td>
              <td><span class="fw-semibold">@${l.username}</span></td>
              <td><code style="font-size:.85rem;color:#0d6efd">${l.maskedEmail || '—'}</code></td>
              <td><span class="badge ${badgeClass(l.action)} rounded-pill">${l.actionLabel}</span></td>
              <td class="text-muted small">${l.ipAddress || '—'}</td>
            </tr>`).join('');
    } catch(e) { tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger py-3">Error al cargar el historial</td></tr>'; }
}

async function loadUsuarios() {
    try {
        allUsers = await api.getUsers();
        const roleColors = { ADMIN:'bg-danger', SUPERVISOR:'bg-warning text-dark', VENDEDOR:'bg-primary' };
        document.getElementById('users-tbody').innerHTML = allUsers.length===0 ? emptyRow(5,'Sin usuarios')
            : allUsers.map(u=>{
                const isSelf = currentUser?.id === u.id || currentUser?.username === u.username;
                const toggleBtn = isSelf
                    ? `<button class="btn btn-xs btn-outline-secondary" disabled title="No puedes desactivar tu propia cuenta"><i class="bi bi-lock"></i></button>`
                    : u.active
                        ? `<button class="btn btn-xs btn-outline-warning" onclick="toggleUser(${u.id},'${u.username}')" title="Desactivar usuario"><i class="bi bi-toggle-on"></i></button>`
                        : `<button class="btn btn-xs btn-outline-success" onclick="toggleUser(${u.id},'${u.username}')" title="Activar usuario"><i class="bi bi-toggle-off"></i></button>`;
                return `<tr>
                    <td class="fw-semibold">${u.username}</td>
                    <td>${u.fullName}</td>
                    <td class="small text-muted">${u.email ? `<i class="bi bi-envelope me-1"></i>${u.email}` : '<span class="text-danger">Sin correo</span>'}</td>
                    <td><span class="badge ${roleColors[u.role]||'bg-secondary'}">${u.role}</span></td>
                    <td><span class="badge ${u.active?'bg-success':'bg-secondary'}">${u.active?'Activo':'Inactivo'}</span></td>
                    <td>
                      <button class="btn btn-xs btn-outline-primary me-1" onclick="openUserModal(${u.id})"><i class="bi bi-pencil"></i></button>
                      ${toggleBtn}
                    </td></tr>`;
            }).join('');
    } catch(e){ toast('Sin permisos para ver usuarios','danger'); }
}

function openUserModal(id=null) {
    document.getElementById('user-id').value=id||'';
    document.getElementById('modal-user-title').textContent=id?'Editar Usuario':'Nuevo Usuario';
    const u=id?allUsers.find(x=>x.id===id):null;
    document.getElementById('user-username').value=u?.username||'';
    document.getElementById('user-fullname').value=u?.fullName||'';
    document.getElementById('user-email').value=u?.email||'';
    document.getElementById('user-password').value='';
    document.getElementById('user-role').value=u?.role||'VENDEDOR';
    document.getElementById('user-username').disabled=!!id;
    getModal('modalUser').show();
}

async function saveUser() {
    const id=document.getElementById('user-id').value;
    const data={username:document.getElementById('user-username').value.trim(), fullName:document.getElementById('user-fullname').value.trim(), email:document.getElementById('user-email').value.trim()||null, password:document.getElementById('user-password').value||null, role:document.getElementById('user-role').value};
    if(!data.fullName){ toast('El nombre es obligatorio','warning'); return; }
    if(!id&&!data.password){ toast('La contraseña es obligatoria para nuevos usuarios','warning'); return; }
    try { id?await api.updateUser(id,data):await api.createUser(data); toast(id?'Usuario actualizado':'Usuario creado'); getModal('modalUser').hide(); loadUsuarios(); }
    catch(e){ toast(e.message,'danger'); }
}

async function toggleUser(id, username) {
    const user = allUsers.find(u => u.id === id);
    const action = user?.active ? 'desactivar' : 'activar';
    if (!confirm(`¿Confirmas ${action} al usuario "${username}"?`)) return;
    try {
        await api.toggleUser(id);
        toast(`Usuario ${action === 'activar' ? 'activado' : 'desactivado'} correctamente`);
        loadUsuarios();
    } catch(e) { toast(e.message || 'Error al actualizar usuario', 'danger'); }
}

// ══════════════════════════════════════════════════════════
// CONTABILIDAD
// ══════════════════════════════════════════════════════════
let chartTrend = null, chartPayment = null, lastReport = null;

const PAY_LABELS = { EFECTIVO:'Efectivo', TARJETA:'Tarjeta', TRANSFERENCIA:'Transferencia', CREDITO:'Crédito' };
const PAY_COLORS = ['#0d6efd','#20c997','#fd7e14','#6f42c1','#dc3545'];
const CHART_COLORS = ['#0d6efd','#20c997','#fd7e14','#6f42c1','#dc3545','#0dcaf0','#ffc107'];

async function initContabilidad() {
    // Fechas por defecto: mes actual
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().slice(0,10);
    const todayStr = today.toISOString().slice(0,10);
    document.getElementById('rep-from').value = firstDay;
    document.getElementById('rep-to').value   = todayStr;

    // Poblar selector de cajas
    try {
        const cajas = await api.getCajas();
        document.getElementById('rep-caja').innerHTML =
            '<option value="">— Todas las cajas —</option>' +
            cajas.map(c=>`<option value="${c.id}">${c.name} — ${c.sellerName||'Sin vendedor'}</option>`).join('');
    } catch(e) {}

    loadContabilidad();
}

async function loadContabilidad() {
    const from  = document.getElementById('rep-from').value;
    const to    = document.getElementById('rep-to').value;
    const cajaId = document.getElementById('rep-caja').value || null;
    if (!from || !to) { toast('Selecciona el rango de fechas','warning'); return; }

    try {
        const r = await api.getAccounting(from, to, cajaId);
        lastReport = r;

        // Stat cards
        document.getElementById('rep-subtotal').textContent = fmt(r.totalSubtotal);
        document.getElementById('rep-tax').textContent      = fmt(r.totalTax);
        document.getElementById('rep-total').textContent    = fmt(r.totalSales);
        document.getElementById('rep-count').textContent    = r.salesCount;

        // Gráfica tendencia diaria
        const trendCtx = document.getElementById('chart-trend').getContext('2d');
        if (chartTrend) chartTrend.destroy();
        chartTrend = new Chart(trendCtx, {
            type: 'line',
            data: {
                labels: r.dailyTrend.map(d => d.date),
                datasets: [{
                    label: 'Ventas del día',
                    data: r.dailyTrend.map(d => Number(d.total)),
                    borderColor: '#0d6efd', backgroundColor: 'rgba(13,110,253,0.1)',
                    fill: true, tension: 0.4, pointRadius: 4
                }]
            },
            options: { responsive:true, plugins:{ legend:{ display:false } },
                scales:{ y:{ ticks:{ callback: v => '$'+Number(v).toLocaleString('es-CO') } } } }
        });

        // Gráfica métodos de pago (dona)
        const payCtx = document.getElementById('chart-payment').getContext('2d');
        if (chartPayment) chartPayment.destroy();
        chartPayment = new Chart(payCtx, {
            type: 'doughnut',
            data: {
                labels: r.byPaymentMethod.map(p => PAY_LABELS[p.method]||p.method),
                datasets: [{ data: r.byPaymentMethod.map(p => Number(p.total)), backgroundColor: PAY_COLORS }]
            },
            options: { responsive:true, plugins:{ legend:{ position:'bottom' } } }
        });

        // Tabla por caja
        const totalVentas = Number(r.totalSales) || 1;
        document.getElementById('rep-tbody').innerHTML = r.byCaja.length === 0
            ? `<tr><td colspan="7" class="text-center text-muted py-3">Sin ventas en el período</td></tr>`
            : r.byCaja.map((c,i) => `<tr>
                <td><span class="fw-semibold">${c.cajaName}</span></td>
                <td>${c.sellerName}</td>
                <td><span class="badge bg-primary">${c.count}</span></td>
                <td>${fmt(c.subtotal)}</td>
                <td class="text-warning fw-semibold">${fmt(c.tax)}</td>
                <td class="text-success fw-bold">${fmt(c.total)}</td>
                <td>
                  <div class="d-flex align-items-center gap-2">
                    <div class="progress flex-grow-1" style="height:6px">
                      <div class="progress-bar" style="width:${(Number(c.total)/totalVentas*100).toFixed(1)}%;background:${CHART_COLORS[i%CHART_COLORS.length]}"></div>
                    </div>
                    <span class="small">${(Number(c.total)/totalVentas*100).toFixed(1)}%</span>
                  </div>
                </td></tr>`).join('');

        document.getElementById('rep-tfoot').innerHTML =
            `<tr><td colspan="2">TOTAL GENERAL</td><td>${r.salesCount}</td><td>${fmt(r.totalSubtotal)}</td><td>${fmt(r.totalTax)}</td><td>${fmt(r.totalSales)}</td><td>100%</td></tr>`;

        // Tabla métodos de pago
        document.getElementById('rep-payment-tbody').innerHTML = r.byPaymentMethod.map(p => `<tr>
            <td><span class="badge bg-secondary">${PAY_LABELS[p.method]||p.method}</span></td>
            <td>${p.count}</td>
            <td class="fw-semibold">${fmt(p.total)}</td>
            <td>${(Number(p.total)/totalVentas*100).toFixed(1)}%</td></tr>`).join('');

    } catch(e) { toast('Error generando reporte: '+e.message,'danger'); }
}

function exportExcel() {
    if (!lastReport) { toast('Genera el reporte primero','warning'); return; }
    const r   = lastReport;
    const from = document.getElementById('rep-from').value;
    const to   = document.getElementById('rep-to').value;
    const emp  = empresaConfig || {};
    const now  = new Date().toLocaleString('es-CO', { dateStyle:'long', timeStyle:'short' });

    const empresa = emp.razonSocial || 'Mi Empresa';
    const nit     = emp.nit     ? `NIT: ${emp.nit}`         : '';
    const tel     = emp.telefono? `Tel: ${emp.telefono}`    : '';
    const dir     = [emp.direccion, emp.ciudad, emp.departamento].filter(Boolean).join(', ');
    const fv      = n => Number(n || 0);
    const totalV  = fv(r.totalSales) || 1;
    const ticket  = r.salesCount > 0 ? Math.round(totalV / r.salesCount) : 0;
    const pct     = (v) => ((fv(v) / totalV) * 100).toFixed(2) + '%';

    // ── Helpers de estilo ────────────────────────────────────────────
    const rgb  = x => ({ rgb: x });
    const fill = x => ({ patternType: 'solid', fgColor: rgb(x) });
    const font = (o={}) => ({ sz:10, color:rgb('111827'), bold:false, italic:false, ...o });
    const aln  = (h='left',v='center',wrap=false) => ({ horizontal:h, vertical:v, wrapText:wrap });
    const bdr  = (s='thin',c='CBD5E1') => ({ style:s, color:rgb(c) });
    const borders = (s='thin',c='CBD5E1') => { const b=bdr(s,c); return {top:b,bottom:b,left:b,right:b}; };
    const bStrong  = () => borders('medium','1E3A5F');
    const bOrange  = () => borders('medium','D97706');

    // Paleta de estilos
    const S = {
        H1:  { font:font({bold:true,sz:18,color:rgb('FFFFFF')}), fill:fill('1E3A5F'), alignment:aln('center','center',true), border:bStrong() },
        H2:  { font:font({bold:true,sz:13,color:rgb('FFFFFF')}), fill:fill('2563EB'), alignment:aln('center'),  border:borders() },
        H3:  { font:font({sz:10,color:rgb('FFFFFF')}),            fill:fill('3B82F6'), alignment:aln('center'),  border:borders() },
        SEC: { font:font({bold:true,sz:11,color:rgb('FFFFFF')}), fill:fill('1E3A5F'), alignment:aln('left'),    border:bStrong() },
        COL: { font:font({bold:true,sz:10,color:rgb('FFFFFF')}), fill:fill('2563EB'), alignment:aln('center'),  border:borders() },
        LBL: { font:font({bold:true,sz:10}), fill:fill('DBEAFE'), alignment:aln('left'),  border:borders() },
        VAL: { font:font({bold:true,sz:11,color:rgb('166534')}), fill:fill('DCFCE7'), alignment:aln('right'),  border:borders(), numFmt:'#,##0.00' },
        VALN:{ font:font({bold:true,sz:11,color:rgb('1E3A5F')}), fill:fill('DBEAFE'), alignment:aln('right'),  border:borders(), numFmt:'#,##0'    },
        D0:  { font:font(), fill:fill('FFFFFF'), alignment:aln('left'),  border:borders() },
        D1:  { font:font(), fill:fill('F1F5F9'), alignment:aln('left'),  border:borders() },
        M0:  { font:font(), fill:fill('FFFFFF'), alignment:aln('right'), border:borders(), numFmt:'#,##0.00' },
        M1:  { font:font(), fill:fill('F1F5F9'), alignment:aln('right'), border:borders(), numFmt:'#,##0.00' },
        TOT: { font:font({bold:true,sz:11,color:rgb('FFFFFF')}), fill:fill('1E3A5F'), alignment:aln('left'),  border:bStrong() },
        T$:  { font:font({bold:true,sz:11,color:rgb('FFFFFF')}), fill:fill('1E3A5F'), alignment:aln('right'), border:bStrong(), numFmt:'#,##0.00' },
        TN:  { font:font({bold:true,sz:11,color:rgb('FFFFFF')}), fill:fill('1E3A5F'), alignment:aln('right'), border:bStrong(), numFmt:'#,##0'    },
        TC:  { font:font({bold:true,sz:11,color:rgb('FFFFFF')}), fill:fill('1E3A5F'), alignment:aln('center'),border:bStrong() },
        IH:  { font:font({bold:true,sz:10,color:rgb('FFFFFF')}), fill:fill('D97706'), alignment:aln('center'), border:bOrange() },
        IM:  { font:font({sz:10}), fill:fill('FEF3C7'), alignment:aln('right'), border:borders('thin','D97706'), numFmt:'#,##0.00' },
        ID:  { font:font({sz:10}), fill:fill('FEF3C7'), alignment:aln('left'),  border:borders('thin','D97706') },
        NOTE:{ font:font({italic:true,sz:9,color:rgb('6B7280')}), fill:fill('F9FAFB'), alignment:aln('left','center',true), border:{} },
        EMPTY:{ font:font(), fill:{patternType:'none'}, alignment:aln(), border:{} },
    };

    // ── Constructores de celdas y hojas ──────────────────────────────
    const c  = (v, s, t) => ({ v: v ?? '', t: t || (typeof v==='number'?'n':'s'), s });
    const em = (s=S.EMPTY) => c('', s);

    function buildSheet(rows, cols, merges=[]) {
        const ws = {};
        rows.forEach((row, ri) => {
            (row || []).forEach((cell, ci) => {
                const ref = XLSX.utils.encode_cell({ r:ri, c:ci });
                ws[ref] = (cell && typeof cell==='object' && 'v' in cell) ? cell : { v:cell??'', t:'s' };
            });
        });
        const maxR = rows.length - 1;
        const maxC = Math.max(...rows.map(r => (r||[]).length), 1) - 1;
        ws['!ref']  = XLSX.utils.encode_range({ s:{r:0,c:0}, e:{r:maxR,c:maxC} });
        ws['!cols'] = cols;
        if (merges.length) ws['!merges'] = merges;
        return ws;
    }
    const mg    = (r,c1,c2) => ({ s:{r,c:c1}, e:{r,c:c2} });
    const mgRow = (r, max)  => mg(r, 0, max);

    // ════════════════════════════════════════════════════════
    // HOJA 1 — RESUMEN EJECUTIVO
    // ════════════════════════════════════════════════════════
    const wb = XLSX.utils.book_new();
    {
        const rows=[], mgs=[], LAST=5;
        const add  = row => { rows.push(row); return rows.length-1; };
        const mAll = ri  => mgs.push(mgRow(ri,LAST));
        const m    = (ri,a,b) => mgs.push(mg(ri,a,b));

        let ri;
        ri=add([c(empresa,S.H1)]); mAll(ri);
        ri=add([c([nit,tel].filter(Boolean).join('  ·  ')||' ',S.H3)]); mAll(ri);
        ri=add([c(dir||' ',S.H3)]); mAll(ri);
        ri=add([c('REPORTE CONTABLE DE VENTAS',S.H2)]); mAll(ri);
        ri=add([c(`Período:  ${from}  al  ${to}`,S.H3),em(S.H3),em(S.H3),c(`Generado: ${now}`,S.H3),em(S.H3),em(S.H3)]);
        m(ri,0,2); m(ri,3,5);
        ri=add([em()]); mAll(ri);

        // KPIs
        ri=add([c('INDICADORES CLAVE DEL PERÍODO',S.SEC)]); mAll(ri);
        ri=add([c('CONCEPTO',S.COL),em(S.COL),c('VALOR (COP $)',S.COL),em(S.COL),c('REFERENCIA CONTABLE',S.COL),em(S.COL)]);
        m(ri,0,1); m(ri,2,3); m(ri,4,5);

        const kpi = (lbl,val,nota,vStyle=S.VAL) => {
            const kr=add([c(lbl,S.LBL),em(S.LBL),c(fv(val),vStyle),em(vStyle),c(nota,S.NOTE),em(S.NOTE)]);
            m(kr,0,1); m(kr,2,3); m(kr,4,5);
        };
        kpi('Ingresos Brutos (Base Gravable)', r.totalSubtotal, 'Cta. 4105 — Ingresos Operacionales');
        kpi('IVA Recaudado (Por Pagar DIAN)',  r.totalTax,      'Cta. 2408 — IVA por Pagar', {...S.VAL, fill:fill('FEF3C7'), font:font({bold:true,sz:11,color:rgb('D97706')})});
        kpi('Total Recaudado (Base + IVA)',    r.totalSales,    'Cta. 1105/1110 — Caja / Bancos');
        kpi('N° de Transacciones',             r.salesCount,    'Ventas registradas en el período', {...S.VALN});
        kpi('Ticket Promedio por Venta',       ticket,          'Total Ingresos ÷ N° Transacciones', {...S.VALN});
        ri=add([em()]); mAll(ri);

        // Por caja
        ri=add([c('VENTAS POR PUNTO DE VENTA / CAJA',S.SEC)]); mAll(ri);
        add([c('CAJA',S.COL),c('VENDEDOR',S.COL),c('N° VENTAS',S.COL),c('SUBTOTAL COP',S.COL),c('IVA COP',S.COL),c('TOTAL COP',S.COL)]);
        r.byCaja.forEach((ca,i) => {
            const D=i%2?S.D1:S.D0, M=i%2?S.M1:S.M0;
            add([c(ca.cajaName,D),c(ca.sellerName||'—',D),c(ca.count,{...D,alignment:aln('center')}),c(fv(ca.subtotal),M),c(fv(ca.tax),M),c(fv(ca.total),M)]);
        });
        ri=add([c('TOTAL GENERAL',S.TOT),em(S.TOT),c(r.salesCount,S.TN),c(fv(r.totalSubtotal),S.T$),c(fv(r.totalTax),S.T$),c(fv(r.totalSales),S.T$)]);
        m(ri,0,1);
        ri=add([em()]); mAll(ri);

        // Métodos de pago
        ri=add([c('VENTAS POR MÉTODO DE PAGO',S.SEC)]); mAll(ri);
        ri=add([c('MÉTODO DE PAGO',S.COL),em(S.COL),c('N° TRANS.',S.COL),c('TOTAL COP',S.COL),c('PARTICIPACIÓN',S.COL),em(S.COL)]);
        m(ri,0,1); m(ri,4,5);
        r.byPaymentMethod.forEach((p,i) => {
            const D=i%2?S.D1:S.D0, M=i%2?S.M1:S.M0;
            const pr=add([c(PAY_LABELS[p.method]||p.method,D),em(D),c(p.count,{...D,alignment:aln('center')}),c(fv(p.total),M),c(pct(p.total),{...D,alignment:aln('center')}),em(D)]);
            m(pr,0,1); m(pr,4,5);
        });
        ri=add([c('TOTAL',S.TOT),em(S.TOT),c(r.salesCount,S.TN),c(fv(r.totalSales),S.T$),c('100.00%',S.TC),em(S.TC)]);
        m(ri,0,1); m(ri,4,5);
        ri=add([em()]); mAll(ri);

        ri=add([c('* Reporte generado automáticamente. Verifique la información con su contador antes de presentar ante la DIAN u otras entidades fiscales. Elaborado bajo principios NIIF para Colombia (Ley 1314/2009).',S.NOTE)]); mAll(ri);

        const ws = buildSheet(rows,[{wch:30},{wch:18},{wch:16},{wch:16},{wch:24},{wch:14}],mgs);
        ws['!rows'] = rows.map((_,i) => i===0?{hpt:42}:i===rows.length-1?{hpt:44}:{hpt:20});
        XLSX.utils.book_append_sheet(wb, ws, 'Resumen Ejecutivo');
    }

    // ════════════════════════════════════════════════════════
    // HOJA 2 — ESTADO DE INGRESOS (NIIF / PUC Colombia)
    // ════════════════════════════════════════════════════════
    {
        const rows=[], mgs=[], LAST=3;
        const add  = row => { rows.push(row); return rows.length-1; };
        const mAll = ri  => mgs.push(mgRow(ri,LAST));

        let ri;
        ri=add([c(empresa,S.H1)]); mAll(ri);
        ri=add([c('ESTADO DE INGRESOS OPERACIONALES',S.H2)]); mAll(ri);
        ri=add([c(`Período: ${from}  al  ${to}   ·   ${nit}`,S.H3)]); mAll(ri);
        ri=add([c('Presentado bajo NIC 18 / NIIF 15 — Reconocimiento de Ingresos (Ley 1314/2009)',S.NOTE)]); mAll(ri);
        ri=add([em()]); mAll(ri);

        add([c('CUENTA PUC',S.COL),c('DESCRIPCIÓN',S.COL),c('DÉBITO COP',S.COL),c('CRÉDITO COP',S.COL),c('SALDO COP',S.COL)]);
        // Cuentas contables colombianas
        const cta = (cod,desc,deb,cre,iva=false) => {
            const D=iva?S.ID:S.D0, M=iva?S.IM:S.M0;
            add([c(cod,{...D,alignment:aln('center')}),c(desc,D),c(deb>0?deb:'',deb>0?M:{...M,numFmt:'@'}),c(cre>0?cre:'',cre>0?M:{...M,numFmt:'@'}),c(deb>0?deb:cre,M)]);
        };
        cta('4105','Comercio al por Menor — Ingresos por Ventas',0,fv(r.totalSubtotal));
        cta('2408','IVA Generado por Pagar',0,fv(r.totalTax),true);
        cta('1105','Caja — Efectivo Recibido',fv(r.totalSales),0);
        ri=add([em()]); mAll(ri);

        ri=add([c('COMPROBANTE DE CUADRE CONTABLE',S.SEC)]); mAll(ri);
        add([c('CONCEPTO',S.COL),c('',S.COL),c('IMPORTE COP',S.COL),c('% SOBRE TOTAL',S.COL),c('',S.COL)]);
        const lin = (lbl,val,p,alt=false) => {
            const D=alt?S.D1:S.D0, M=alt?S.M1:S.M0;
            add([c(lbl,D),em(D),c(fv(val),M),c(p,{...D,alignment:aln('center')}),em(D)]);
        };
        lin('Ingresos Brutos (sin IVA)',  r.totalSubtotal, pct(r.totalSubtotal));
        lin('IVA Recaudado (19%)',        r.totalTax,      pct(r.totalTax),  true);
        lin('Total Recaudado',            r.totalSales,    '100.00%');
        ri=add([em()]); mAll(ri);

        ri=add([c('TOTAL INGRESOS OPERACIONALES',S.TOT),em(S.TOT),c(fv(r.totalSubtotal),S.T$),c(pct(r.totalSubtotal),S.TC),em(S.TOT)]);
        ri=add([c('TOTAL IVA POR PAGAR (DIAN)',{...S.TOT,fill:fill('D97706')}),em({...S.TOT,fill:fill('D97706')}),c(fv(r.totalTax),{...S.T$,fill:fill('D97706')}),c(pct(r.totalTax),{...S.TC,fill:fill('D97706')}),em({...S.TC,fill:fill('D97706')})]);
        ri=add([c('TOTAL RECAUDO BRUTO',S.TOT),em(S.TOT),c(fv(r.totalSales),S.T$),c('100.00%',S.TC),em(S.TOT)]);
        ri=add([em()]); mAll(ri);
        ri=add([c('NOTA LEGAL: Las cifras presentadas corresponden a ingresos operacionales reconocidos bajo NIIF 15. El IVA recaudado (Cta. 2408) constituye un pasivo fiscal que debe declararse y pagarse ante la DIAN según el período fiscal. Para declaración de renta, use el formulario 110. Para IVA bimestral/cuatrimestral, formulario 300.',S.NOTE)]); mAll(ri);

        const ws = buildSheet(rows,[{wch:12},{wch:38},{wch:18},{wch:18},{wch:18}],mgs);
        ws['!rows'] = rows.map((_,i) => i===0?{hpt:42}:i===rows.length-1?{hpt:80}:{hpt:20});
        XLSX.utils.book_append_sheet(wb, ws, 'Estado de Ingresos');
    }

    // ════════════════════════════════════════════════════════
    // HOJA 3 — LIBRO AUXILIAR DE VENTAS (Art. 509 E.T.)
    // ════════════════════════════════════════════════════════
    {
        const rows=[], mgs=[], LAST=6;
        const add  = row => { rows.push(row); return rows.length-1; };
        const mAll = ri  => mgs.push(mgRow(ri,LAST));

        let ri;
        ri=add([c(empresa,S.H1)]); mAll(ri);
        ri=add([c('LIBRO AUXILIAR DE VENTAS — DECLARACIÓN DE IVA',S.IH)]); mAll(ri);
        ri=add([c(`Período Fiscal: ${from}  al  ${to}   ·   ${nit}`,S.H3)]); mAll(ri);
        ri=add([c('Art. 509 Estatuto Tributario — Obligación de llevar libro fiscal de operaciones diarias',S.NOTE)]); mAll(ri);
        ri=add([em()]); mAll(ri);

        add([c('FECHA',S.IH),c('N° TRANS.',S.IH),c('BASE 0%',S.IH),c('BASE 5%',S.IH),c('BASE 19%',S.IH),c('IVA TOTAL',S.IH),c('TOTAL FACT.',S.IH)]);

        const taxR  = totalV > 0 ? fv(r.totalTax)      / totalV : 0;
        const baseR = totalV > 0 ? fv(r.totalSubtotal) / totalV : 1;
        r.dailyTrend.forEach((d,i) => {
            const D=i%2?S.D1:S.D0, M=i%2?S.M1:S.M0;
            const tot=fv(d.total), tax=tot*taxR, base=tot*baseR;
            add([c(d.date,D),c(d.count,{...D,alignment:aln('center')}),c(0,M),c(0,M),c(base,M),c(tax,M),c(tot,M)]);
        });
        ri=add([c('TOTALES',S.TOT),c(r.salesCount,S.TN),c(0,S.T$),c(0,S.T$),c(fv(r.totalSubtotal),S.T$),c(fv(r.totalTax),S.T$),c(fv(r.totalSales),S.T$)]);
        ri=add([em()]); mAll(ri);
        ri=add([c('AVISO: El desglose por tarifa IVA (0% / 5% / 19%) requiere registro individual por factura. Los valores aquí son una aproximación proporcional basada en el promedio del período. Consulte a su contador para la declaración oficial de IVA (Formulario 300 DIAN).',S.NOTE)]); mAll(ri);

        const ws = buildSheet(rows,[{wch:14},{wch:10},{wch:16},{wch:16},{wch:16},{wch:14},{wch:16}],mgs);
        ws['!rows'] = rows.map((_,i) => i===0?{hpt:42}:i===rows.length-1?{hpt:68}:{hpt:20});
        XLSX.utils.book_append_sheet(wb, ws, 'Libro IVA');
    }

    // ════════════════════════════════════════════════════════
    // HOJA 4 — TENDENCIA DIARIA
    // ════════════════════════════════════════════════════════
    {
        const rows=[], mgs=[], LAST=4;
        const add  = row => { rows.push(row); return rows.length-1; };
        const mAll = ri  => mgs.push(mgRow(ri,LAST));

        let ri;
        ri=add([c(empresa,S.H1)]); mAll(ri);
        ri=add([c('TENDENCIA DE VENTAS DIARIAS',S.H2)]); mAll(ri);
        ri=add([c(`Período: ${from}  al  ${to}`,S.H3)]); mAll(ri);
        ri=add([em()]); mAll(ri);
        add([c('FECHA',S.COL),c('N° VENTAS',S.COL),c('TOTAL DÍA COP',S.COL),c('% DEL PERÍODO',S.COL),c('ACUMULADO COP',S.COL)]);

        let acum=0;
        r.dailyTrend.forEach((d,i) => {
            acum += fv(d.total);
            const D=i%2?S.D1:S.D0, M=i%2?S.M1:S.M0;
            add([c(d.date,D),c(d.count,{...D,alignment:aln('center')}),c(fv(d.total),M),c(pct(d.total),{...D,alignment:aln('center')}),c(acum,M)]);
        });
        add([c('TOTAL PERÍODO',S.TOT),c(r.salesCount,S.TN),c(fv(r.totalSales),S.T$),c('100.00%',S.TC),c(fv(r.totalSales),S.T$)]);

        const ws = buildSheet(rows,[{wch:14},{wch:12},{wch:18},{wch:16},{wch:20}],mgs);
        ws['!rows'] = rows.map((_,i) => i===0?{hpt:42}:{hpt:20});
        XLSX.utils.book_append_sheet(wb, ws, 'Tendencia Diaria');
    }

    // ════════════════════════════════════════════════════════
    // HOJA 5 — RENDIMIENTO POR CAJA
    // ════════════════════════════════════════════════════════
    {
        const rows=[], mgs=[], LAST=6;
        const add  = row => { rows.push(row); return rows.length-1; };
        const mAll = ri  => mgs.push(mgRow(ri,LAST));
        const m    = (ri,a,b) => mgs.push(mg(ri,a,b));

        let ri;
        ri=add([c(empresa,S.H1)]); mAll(ri);
        ri=add([c('RENDIMIENTO POR PUNTO DE VENTA',S.H2)]); mAll(ri);
        ri=add([c(`Período: ${from}  al  ${to}`,S.H3)]); mAll(ri);
        ri=add([em()]); mAll(ri);
        add([c('CAJA',S.COL),c('VENDEDOR',S.COL),c('N° VENTAS',S.COL),c('SUBTOTAL COP',S.COL),c('IVA COP',S.COL),c('TOTAL COP',S.COL),c('PARTICIPACIÓN',S.COL)]);

        r.byCaja.forEach((ca,i) => {
            const D=i%2?S.D1:S.D0, M=i%2?S.M1:S.M0;
            add([c(ca.cajaName,D),c(ca.sellerName||'—',D),c(ca.count,{...D,alignment:aln('center')}),c(fv(ca.subtotal),M),c(fv(ca.tax),M),c(fv(ca.total),M),c(pct(ca.total),{...D,alignment:aln('center')})]);
        });
        ri=add([c('TOTAL GENERAL',S.TOT),em(S.TOT),c(r.salesCount,S.TN),c(fv(r.totalSubtotal),S.T$),c(fv(r.totalTax),S.T$),c(fv(r.totalSales),S.T$),c('100.00%',S.TC)]);
        m(ri,0,1);

        const ws = buildSheet(rows,[{wch:22},{wch:20},{wch:12},{wch:18},{wch:16},{wch:18},{wch:14}],mgs);
        ws['!rows'] = rows.map((_,i) => i===0?{hpt:42}:{hpt:20});
        XLSX.utils.book_append_sheet(wb, ws, 'Por Caja');
    }

    const fname = `ReporteContable_${empresa.replace(/[^a-zA-Z0-9]/g,'_')}_${from}_${to}.xlsx`;
    XLSX.writeFile(wb, fname);
    toast('Excel generado: ' + fname);
}

function exportPDF() {
    if (!lastReport) { toast('Genera el reporte primero','warning'); return; }
    const r = lastReport;
    const e = empresaConfig || {};
    const from = document.getElementById('rep-from').value;
    const to   = document.getElementById('rep-to').value;
    const totalVentas = Number(r.totalSales) || 1;

    const win = window.open('','_blank','width=900,height=700');
    win.document.write(`<!DOCTYPE html><html><head><meta charset="UTF-8">
    <title>Reporte Contable ${from} - ${to}</title>
    <style>
    *{box-sizing:border-box}body{font-family:'Segoe UI',sans-serif;margin:0;padding:28px;color:#222;font-size:13px}
    h1{font-size:20px;color:#1a2540;margin:0}h2{font-size:14px;color:#1a2540;margin:20px 0 8px;border-bottom:2px solid #1a2540;padding-bottom:4px}
    .header{display:flex;justify-content:space-between;align-items:start;margin-bottom:16px}
    .badge-box{background:#1a2540;color:white;padding:10px 18px;border-radius:8px;text-align:right}
    .cards{display:flex;gap:12px;margin-bottom:20px}
    .card{flex:1;background:#f8f9fa;border-radius:8px;padding:14px;text-align:center}
    .card .val{font-size:18px;font-weight:700;color:#1a2540;margin-top:4px}
    .card .lbl{font-size:11px;color:#888}
    table{width:100%;border-collapse:collapse;font-size:12px;margin-bottom:16px}
    th{background:#1a2540;color:white;padding:7px 10px;text-align:left}
    td{padding:6px 10px;border-bottom:1px solid #eee}
    .tfoot td{background:#f0f4ff;font-weight:700}
    .footer{text-align:center;color:#aaa;font-size:11px;margin-top:24px;border-top:1px solid #eee;padding-top:10px}
    @media print{body{padding:16px}}
    </style></head><body>
    <div class="header">
      <div><h1>${e.razonSocial||'Mi Empresa'}</h1><p style="margin:2px 0;color:#666">${e.nit?'NIT '+e.nit+'-'+e.digitoVerificacion:''}</p><p style="margin:2px 0;color:#666">${e.city||''}</p></div>
      <div class="badge-box"><div style="font-size:16px;font-weight:700">REPORTE CONTABLE</div><div style="font-size:12px;opacity:.85">Del ${from} al ${to}</div></div>
    </div>
    <div class="cards">
      <div class="card"><div class="lbl">Ventas Netas</div><div class="val">${fmt(r.totalSubtotal)}</div></div>
      <div class="card"><div class="lbl">Total IVA</div><div class="val">${fmt(r.totalTax)}</div></div>
      <div class="card"><div class="lbl">Total Ingresos</div><div class="val" style="color:#198754">${fmt(r.totalSales)}</div></div>
      <div class="card"><div class="lbl">N° Ventas</div><div class="val">${r.salesCount}</div></div>
    </div>
    <h2>Balance por Caja</h2>
    <table><thead><tr><th>Caja</th><th>Vendedor</th><th>N° Ventas</th><th>Subtotal</th><th>IVA</th><th>Total</th><th>%</th></tr></thead>
    <tbody>${r.byCaja.map(c=>`<tr><td>${c.cajaName}</td><td>${c.sellerName}</td><td>${c.count}</td><td>${fmt(c.subtotal)}</td><td>${fmt(c.tax)}</td><td><b>${fmt(c.total)}</b></td><td>${(Number(c.total)/totalVentas*100).toFixed(1)}%</td></tr>`).join('')}</tbody>
    <tfoot><tr class="tfoot"><td colspan="2">TOTAL</td><td>${r.salesCount}</td><td>${fmt(r.totalSubtotal)}</td><td>${fmt(r.totalTax)}</td><td>${fmt(r.totalSales)}</td><td>100%</td></tr></tfoot></table>
    <h2>Ventas por Método de Pago</h2>
    <table><thead><tr><th>Método</th><th>N° Ventas</th><th>Total</th><th>%</th></tr></thead>
    <tbody>${r.byPaymentMethod.map(p=>`<tr><td>${PAY_LABELS[p.method]||p.method}</td><td>${p.count}</td><td><b>${fmt(p.total)}</b></td><td>${(Number(p.total)/totalVentas*100).toFixed(1)}%</td></tr>`).join('')}</tbody></table>
    <h2>Tendencia Diaria</h2>
    <table><thead><tr><th>Fecha</th><th>N° Ventas</th><th>Total</th></tr></thead>
    <tbody>${r.dailyTrend.map(d=>`<tr><td>${d.date}</td><td>${d.count}</td><td>${fmt(d.total)}</td></tr>`).join('')}</tbody></table>
    <div class="footer">${e.razonSocial||'Inventario Pro'} — Reporte generado el ${new Date().toLocaleDateString('es-CO')}</div>
    <script>window.onload=()=>{window.print();}<\/script></body></html>`);
    win.document.close();
}

// ══════════════════════════════════════════════════════════
// EMPRESA
// ══════════════════════════════════════════════════════════
async function loadEmpresa() {
    try {
        const e = await api.getEmpresa();
        empresaConfig = e;
        document.getElementById('emp-razonSocial').value = e.razonSocial||'';
        document.getElementById('emp-nit').value = e.nit||'';
        document.getElementById('emp-dv').value = e.digitoVerificacion||'';
        document.getElementById('emp-address').value = e.address||'';
        document.getElementById('emp-city').value = e.city||'';
        document.getElementById('emp-dept').value = e.department||'';
        document.getElementById('emp-phone').value = e.phone||'';
        document.getElementById('emp-email').value = e.email||'';
        document.getElementById('emp-web').value = e.website||'';
        document.getElementById('emp-regime').value = e.regime||'RESPONSABLE_IVA';
        document.getElementById('emp-prefix').value = e.invoicePrefix||'FV';
        document.getElementById('emp-dianRes').value = e.dianResolutionNumber||'';
        document.getElementById('emp-dianDate').value = e.dianResolutionDate||'';
        document.getElementById('emp-rangeFrom').value = e.dianRangeFrom||'';
        document.getElementById('emp-rangeTo').value = e.dianRangeTo||'';
        document.getElementById('emp-footer').value = e.ticketFooter||'';
        document.getElementById('emp-mailUser').value = e.mailUsername||'';
        document.getElementById('emp-mailFrom').value = e.mailFromName||'';
        document.getElementById('emp-mailPass').value = '';
        const badge = document.getElementById('mail-status-badge');
        if (e.mailConfigured) badge.classList.remove('d-none'); else badge.classList.add('d-none');
    } catch(e) { toast('Error cargando configuración de empresa: '+e.message,'danger'); }
}

async function saveEmpresa() {
    const data = {
        razonSocial: document.getElementById('emp-razonSocial').value.trim(),
        nit: document.getElementById('emp-nit').value.trim(),
        digitoVerificacion: document.getElementById('emp-dv').value.trim(),
        address: document.getElementById('emp-address').value.trim(),
        city: document.getElementById('emp-city').value.trim(),
        department: document.getElementById('emp-dept').value.trim(),
        phone: document.getElementById('emp-phone').value.trim(),
        email: document.getElementById('emp-email').value.trim(),
        website: document.getElementById('emp-web').value.trim(),
        regime: document.getElementById('emp-regime').value,
        invoicePrefix: document.getElementById('emp-prefix').value.trim()||'FV',
        dianResolutionNumber: document.getElementById('emp-dianRes').value.trim(),
        dianResolutionDate: document.getElementById('emp-dianDate').value||null,
        dianRangeFrom: parseInt(document.getElementById('emp-rangeFrom').value)||null,
        dianRangeTo: parseInt(document.getElementById('emp-rangeTo').value)||null,
        ticketFooter: document.getElementById('emp-footer').value.trim(),
        mailUsername: document.getElementById('emp-mailUser').value.trim(),
        mailPassword: document.getElementById('emp-mailPass').value || null,
        mailFromName: document.getElementById('emp-mailFrom').value.trim(),
    };
    if(!data.razonSocial){ toast('La razón social es obligatoria','warning'); return; }
    try { empresaConfig = await api.saveEmpresa(data); toast('Configuración guardada'); }
    catch(e){ toast(e.message,'danger'); }
}

// ══════════════════════════════════════════════════════════
// DEVOLUCIONES
// ══════════════════════════════════════════════════════════
let currentReturnSale = null;

const RETURN_STATUS_BADGE = {
    PENDIENTE: '<span class="badge bg-warning text-dark">Pendiente</span>',
    APROBADA:  '<span class="badge bg-success">Aprobada</span>',
    RECHAZADA: '<span class="badge bg-danger">Rechazada</span>'
};

async function loadDevoluciones() {
    const isSupervisorOrAdmin = isAdminOrSupervisor();
    // Panel de pendientes solo para SUPERVISOR/ADMIN
    document.getElementById('returns-pending-panel').classList.toggle('d-none', !isSupervisorOrAdmin);

    if (isSupervisorOrAdmin) {
        document.getElementById('returns-history-title').innerHTML = '<i class="bi bi-clock-history text-primary me-2"></i>Historial Completo';
        await Promise.all([loadReturnsPending(), loadReturnsHistory()]);
    } else {
        document.getElementById('returns-history-title').innerHTML = '<i class="bi bi-clock-history text-primary me-2"></i>Mis Solicitudes';
        await loadMyReturns();
    }
}

async function loadReturnsPending() {
    try {
        const pending = await api.getReturnsPending();
        const badge = document.getElementById('returns-pending-badge');
        badge.textContent = pending.length;
        badge.classList.toggle('d-none', pending.length === 0);

        document.getElementById('returns-pending-tbody').innerHTML = pending.length === 0
            ? emptyRow(6, 'Sin solicitudes pendientes')
            : pending.map(r => `<tr>
                <td class="fw-semibold small">${r.invoiceNumber || 'Venta #' + r.saleId}</td>
                <td class="text-muted small">${r.requestedBy || '—'}</td>
                <td class="small">${r.reason}</td>
                <td class="fw-semibold text-warning">${fmt(r.refundTotal)}</td>
                <td class="text-muted small">${fmtDate(r.requestDate)}</td>
                <td>
                  <div class="d-flex gap-1">
                    <button class="btn btn-xs btn-success" onclick="approveReturn(${r.id})" title="Aprobar"><i class="bi bi-check-lg"></i></button>
                    <button class="btn btn-xs btn-danger" onclick="openRejectModal(${r.id})" title="Rechazar"><i class="bi bi-x-lg"></i></button>
                    <button class="btn btn-xs btn-outline-secondary" onclick="viewReturn(${r.id})" title="Ver detalle"><i class="bi bi-eye"></i></button>
                  </div>
                </td>
              </tr>`).join('');
    } catch(e) { toast('Error al cargar pendientes: ' + e.message, 'danger'); }
}

async function loadReturnsHistory() {
    try {
        const returns = await api.getReturns();
        renderReturns(returns);
    } catch(e) { toast('Error al cargar historial: ' + e.message, 'danger'); }
}

async function loadMyReturns() {
    try {
        const returns = await api.getMyReturns();
        renderReturns(returns);
    } catch(e) { toast('Error al cargar solicitudes: ' + e.message, 'danger'); }
}

function renderReturns(returns) {
    document.getElementById('returns-tbody').innerHTML = returns.length === 0
        ? emptyRow(6, 'Sin registros')
        : returns.map(r => `<tr>
            <td class="fw-semibold small">${r.invoiceNumber || 'Venta #' + r.saleId}</td>
            <td class="text-muted small">${r.reason}</td>
            <td class="fw-semibold">${fmt(r.refundTotal)}</td>
            <td>${RETURN_STATUS_BADGE[r.status] || r.status}</td>
            <td class="text-muted small">${fmtDate(r.requestDate)}</td>
            <td><button class="btn btn-xs btn-outline-secondary" onclick="viewReturn(${r.id})"><i class="bi bi-eye"></i></button></td>
          </tr>`).join('');
}

async function filterReturnsHistory() {
    const from = document.getElementById('return-date-from').value;
    const to = document.getElementById('return-date-to').value;
    const isAdmin = isAdminOrSupervisor();
    try {
        const returns = isAdmin ? await api.getReturns() : await api.getMyReturns();
        if (!from && !to) { renderReturns(returns); return; }
        renderReturns(returns.filter(r => {
            const d = new Date(r.requestDate);
            if (from && d < new Date(from)) return false;
            if (to && d > new Date(to + 'T23:59:59')) return false;
            return true;
        }));
    } catch(e) { toast('Error al filtrar: ' + e.message, 'danger'); }
}

// ── Buscar venta para solicitar devolución ────────────────
let returnSearchTimeout = null;
function searchSalesForReturn() {
    clearTimeout(returnSearchTimeout);
    const q = document.getElementById('return-invoice-search').value.trim();
    if (!q) { document.getElementById('return-sale-results').classList.add('d-none'); return; }
    returnSearchTimeout = setTimeout(async () => {
        try {
            const sales = await api.getSalesByInvoice(q);
            const list = document.getElementById('return-sale-list');
            list.innerHTML = sales.length === 0
                ? '<div class="list-group-item text-muted small">Sin resultados</div>'
                : sales.map(s => `
                    <button class="list-group-item list-group-item-action py-2 px-3" onclick="selectSaleForReturn(${s.id})">
                      <div class="d-flex justify-content-between">
                        <span class="fw-semibold small">${s.invoiceNumber || 'Venta #' + s.id}</span>
                        <span class="text-success small fw-bold">${fmt(s.total)}</span>
                      </div>
                      <div class="text-muted" style="font-size:.75rem">${s.customerName || 'Sin cliente'} · ${fmtDate(s.date)}</div>
                    </button>`).join('');
            document.getElementById('return-sale-results').classList.remove('d-none');
        } catch(e) { toast('Error: ' + e.message, 'danger'); }
    }, 350);
}

async function selectSaleForReturn(saleId) {
    try {
        currentReturnSale = await api.getSale(saleId);
        const already = await api.getReturnsBySale(saleId);
        document.getElementById('return-invoice-search').value = currentReturnSale.invoiceNumber || '';
        document.getElementById('return-sale-results').classList.add('d-none');

        // Solo contar devoluciones APROBADAS para calcular máximo disponible
        const returnedQty = {};
        already.filter(r => r.status === 'APROBADA').forEach(r => r.items.forEach(i => {
            returnedQty[i.productId] = (returnedQty[i.productId] || 0) + i.quantity;
        }));

        const s = currentReturnSale;
        document.getElementById('return-sale-info').innerHTML = `
          <div class="fw-semibold">${s.invoiceNumber || 'Venta #' + s.id} · ${fmtDate(s.date)}</div>
          <div class="text-muted">${s.customerName || 'Sin cliente'} · ${s.sellerName || ''} · <b class="text-success">${fmt(s.total)}</b></div>`;

        document.getElementById('return-items-tbody').innerHTML = s.items.map(i => {
            const returned = returnedQty[i.productId] || 0;
            const max = i.quantity - returned;
            if (max <= 0)
                return `<tr class="text-muted"><td colspan="3">${i.productName} <span class="badge bg-secondary">Ya devuelto</span></td></tr>`;
            return `<tr>
              <td class="small">${i.productName}</td>
              <td><input type="number" class="form-control form-control-sm return-qty-input"
                   data-product-id="${i.productId}" data-unit-price="${i.unitPrice}"
                   min="0" max="${max}" value="0" style="width:60px" oninput="updateReturnTotal()"></td>
              <td class="text-muted small">${max}</td>
            </tr>`;
        }).join('');

        document.getElementById('return-total-preview').textContent = '$0.00';
        document.getElementById('return-form-card').classList.remove('d-none');
    } catch(e) { toast('Error al cargar la venta: ' + e.message, 'danger'); }
}

function updateReturnTotal() {
    let total = 0;
    document.querySelectorAll('.return-qty-input').forEach(input => {
        total += (parseInt(input.value) || 0) * (parseFloat(input.dataset.unitPrice) || 0);
    });
    document.getElementById('return-total-preview').textContent = fmt(total);
}

function clearReturnForm() {
    currentReturnSale = null;
    document.getElementById('return-form-card').classList.add('d-none');
    document.getElementById('return-invoice-search').value = '';
    document.getElementById('return-reason').value = '';
    document.getElementById('return-notes').value = '';
}

async function submitReturn() {
    if (!currentReturnSale) return;
    const reason = document.getElementById('return-reason').value.trim();
    if (!reason) { toast('El motivo es obligatorio', 'warning'); return; }
    const items = [];
    document.querySelectorAll('.return-qty-input').forEach(input => {
        const qty = parseInt(input.value) || 0;
        if (qty > 0) items.push({ productId: parseInt(input.dataset.productId), quantity: qty });
    });
    if (items.length === 0) { toast('Selecciona al menos un ítem con cantidad > 0', 'warning'); return; }
    try {
        await api.createReturn({
            saleId: currentReturnSale.id, reason,
            notes: document.getElementById('return-notes').value.trim() || null,
            items
        });
        toast('Solicitud enviada — queda pendiente de autorización', 'success');
        clearReturnForm();
        await loadDevoluciones();
    } catch(e) { toast(e.message, 'danger'); }
}

// ── Aprobar / Rechazar (SUPERVISOR y ADMIN) ───────────────
async function approveReturn(id) {
    try {
        await api.approveReturn(id);
        toast('Devolución aprobada — stock restaurado', 'success');
        await loadDevoluciones();
    } catch(e) { toast(e.message, 'danger'); }
}

function openRejectModal(id) {
    document.getElementById('reject-return-id').value = id;
    document.getElementById('reject-reason-input').value = '';
    getModal('modalRejectReturn').show();
}

async function confirmRejectReturn() {
    const id = document.getElementById('reject-return-id').value;
    const reason = document.getElementById('reject-reason-input').value.trim();
    if (!reason) { toast('Escribe el motivo del rechazo', 'warning'); return; }
    try {
        await api.rejectReturn(id, { rejectionReason: reason });
        toast('Solicitud rechazada');
        getModal('modalRejectReturn').hide();
        await loadDevoluciones();
    } catch(e) { toast(e.message, 'danger'); }
}

// ── Ver detalle ───────────────────────────────────────────
async function viewReturn(id) {
    try {
        const r = await api.getReturn(id);
        const statusBadge = RETURN_STATUS_BADGE[r.status] || r.status;
        document.getElementById('return-detail-body').innerHTML = `
          <div class="row mb-3">
            <div class="col">
              <div class="fw-bold">Solicitud #${r.id} ${statusBadge}</div>
              <div class="text-muted small">Solicitada: ${fmtDate(r.requestDate)}</div>
              ${r.processedDate ? `<div class="text-muted small">Procesada: ${fmtDate(r.processedDate)}</div>` : ''}
            </div>
            <div class="col text-end">
              <div class="small">Factura: <b>${r.invoiceNumber || 'Venta #' + r.saleId}</b></div>
              ${r.requestedBy ? `<div class="small text-muted">Solicitado por: ${r.requestedBy}</div>` : ''}
              ${r.processedBy ? `<div class="small text-muted">Autorizado por: ${r.processedBy}</div>` : ''}
            </div>
          </div>
          <div class="mb-2 p-2 bg-light rounded small">
            <b>Motivo:</b> ${r.reason}
            ${r.notes ? '<br><b>Notas:</b> ' + r.notes : ''}
            ${r.rejectionReason ? `<br><b class="text-danger">Rechazo:</b> ${r.rejectionReason}` : ''}
          </div>
          <table class="table table-sm">
            <thead class="table-light"><tr><th>Producto</th><th>Cant.</th><th>Precio</th><th>Subtotal</th></tr></thead>
            <tbody>${r.items.map(i => `<tr><td>${i.productName}</td><td>${i.quantity}</td><td>${fmt(i.unitPrice)}</td><td>${fmt(i.subtotal)}</td></tr>`).join('')}</tbody>
            <tfoot><tr class="fw-bold table-light"><td colspan="3" class="text-end">REEMBOLSO</td><td class="${r.status==='APROBADA'?'text-success':'text-muted'}">${fmt(r.refundTotal)}</td></tr></tfoot>
          </table>`;
        getModal('modalReturnDetail').show();
    } catch(e) { toast(e.message, 'danger'); }
}

// ══════════════════════════════════════════════════════════
// COMPRAS
// ══════════════════════════════════════════════════════════
let allPurchaseItems = [];

async function loadCompras() {
    try {
        const orders = await api.getPurchases();
        renderPurchases(orders);
    } catch(e) { toast('Error compras: ' + e.message, 'danger'); }
}

const PURCHASE_STATUS_BADGE = {
    PENDIENTE: '<span class="badge bg-warning text-dark">Pendiente</span>',
    ENVIADA:   '<span class="badge bg-info text-dark">Enviada</span>',
    RECIBIDA:  '<span class="badge bg-success">Recibida</span>',
    CANCELADA: '<span class="badge bg-secondary">Cancelada</span>',
};

function renderPurchases(orders) {
    document.getElementById('purchases-tbody').innerHTML = orders.length === 0
        ? emptyRow(8, 'Sin órdenes de compra')
        : orders.map(o => `<tr>
            <td class="fw-semibold">${o.orderNumber}</td>
            <td>${o.supplierName || '—'}</td>
            <td>${o.createdByUsername || '—'}</td>
            <td>${PURCHASE_STATUS_BADGE[o.status] || o.status}</td>
            <td class="fw-bold">${fmt(o.total)}</td>
            <td class="text-muted small">${fmtDate(o.createdAt)}</td>
            <td class="text-muted small">${o.expectedDeliveryDate || '—'}</td>
            <td>
              ${o.status === 'PENDIENTE' ? `<button class="btn btn-xs btn-success me-1" onclick="receivePurchase(${o.id})"><i class="bi bi-check-lg"></i> Recibir</button>` : ''}
              ${o.status !== 'RECIBIDA' && o.status !== 'CANCELADA' ? `<button class="btn btn-xs btn-outline-danger" onclick="cancelPurchase(${o.id})"><i class="bi bi-x"></i></button>` : ''}
            </td>
          </tr>`).join('');
}

async function openPurchaseModal() {
    allPurchaseItems = [];
    document.getElementById('purchase-items-tbody').innerHTML = '';
    document.getElementById('purchase-total-preview').textContent = '$0.00';
    document.getElementById('purchase-notes').value = '';
    document.getElementById('purchase-delivery-date').value = '';
    try {
        const [suppliers, products] = await Promise.all([api.getSuppliers(), api.getProducts()]);
        const sel = document.getElementById('purchase-supplier');
        sel.innerHTML = '<option value="">— Sin proveedor —</option>' + suppliers.filter(s=>s.active).map(s=>`<option value="${s.id}">${s.name}</option>`).join('');
        window._purchaseProducts = products.filter(p => p.active);
    } catch(e) { toast('Error: ' + e.message, 'danger'); return; }
    getModal('modalPurchase').show();
}

function addPurchaseItemRow() {
    const products = window._purchaseProducts || [];
    const rowId = Date.now();
    const row = document.createElement('tr');
    row.id = `pi-${rowId}`;
    row.innerHTML = `
      <td><select class="form-select form-select-sm" onchange="updatePurchaseSubtotal('${rowId}')">
        <option value="">— Seleccionar —</option>
        ${products.map(p => `<option value="${p.id}" data-price="${p.price}">${p.name}</option>`).join('')}
      </select></td>
      <td><input type="number" class="form-control form-control-sm" min="1" value="1" onchange="updatePurchaseSubtotal('${rowId}')"></td>
      <td><input type="number" class="form-control form-control-sm" min="0" step="0.01" placeholder="0.00" onchange="updatePurchaseSubtotal('${rowId}')"></td>
      <td class="fw-semibold subtotal-cell">$0.00</td>
      <td><button class="btn btn-xs btn-outline-danger" onclick="document.getElementById('pi-${rowId}').remove();recalcPurchaseTotal()"><i class="bi bi-trash"></i></button></td>`;
    document.getElementById('purchase-items-tbody').appendChild(row);
}

function updatePurchaseSubtotal(rowId) {
    const row = document.getElementById(`pi-${rowId}`);
    if (!row) return;
    const qty = parseFloat(row.querySelector('input[type="number"]').value) || 0;
    const cost = parseFloat(row.querySelectorAll('input[type="number"]')[1].value) || 0;
    row.querySelector('.subtotal-cell').textContent = fmt(qty * cost);
    recalcPurchaseTotal();
}

function recalcPurchaseTotal() {
    let total = 0;
    document.querySelectorAll('#purchase-items-tbody .subtotal-cell').forEach(td => {
        total += parseFloat(td.textContent.replace(/[$,]/g,'')) || 0;
    });
    document.getElementById('purchase-total-preview').textContent = fmt(total);
}

async function savePurchaseOrder() {
    const rows = document.querySelectorAll('#purchase-items-tbody tr');
    const items = [];
    let valid = true;
    rows.forEach(row => {
        const productId = row.querySelector('select')?.value;
        const inputs = row.querySelectorAll('input[type="number"]');
        const qty = parseInt(inputs[0]?.value);
        const cost = parseFloat(inputs[1]?.value);
        if (!productId || !qty || !cost) { valid = false; return; }
        items.push({ productId: parseInt(productId), quantity: qty, unitCost: cost });
    });
    if (!valid || items.length === 0) { toast('Completa todos los ítems correctamente', 'warning'); return; }
    const supplierId = document.getElementById('purchase-supplier').value || null;
    const deliveryDate = document.getElementById('purchase-delivery-date').value || null;
    const notes = document.getElementById('purchase-notes').value || null;
    try {
        await api.createPurchase({ supplierId: supplierId ? parseInt(supplierId) : null, expectedDeliveryDate: deliveryDate, notes, items });
        getModal('modalPurchase').hide();
        toast('Orden de compra creada', 'success');
        loadCompras();
    } catch(e) { toast(e.message, 'danger'); }
}

async function receivePurchase(id) {
    if (!confirm('¿Confirmar recepción? Esto sumará el stock a los productos.')) return;
    try {
        await api.receivePurchase(id);
        toast('Orden recibida — stock actualizado', 'success');
        loadCompras();
    } catch(e) { toast(e.message, 'danger'); }
}

async function cancelPurchase(id) {
    if (!confirm('¿Cancelar esta orden de compra?')) return;
    try {
        await api.cancelPurchase(id);
        toast('Orden cancelada', 'warning');
        loadCompras();
    } catch(e) { toast(e.message, 'danger'); }
}

// ══════════════════════════════════════════════════════════
// AUDITORÍA
// ══════════════════════════════════════════════════════════
let auditPage = 0;
let auditCurrentLogs = [];

const AUDIT_SEVERITY = {
    VENTA_CREADA:           { level:'NORMAL',   badge:'bg-success',  icon:'bi-cart-check-fill',      label:'Normal'   },
    DEVOLUCION_SOLICITADA:  { level:'MEDIO',    badge:'bg-warning text-dark', icon:'bi-arrow-return-left', label:'Medio' },
    DEVOLUCION_APROBADA:    { level:'MEDIO',    badge:'bg-warning text-dark', icon:'bi-check-circle',      label:'Medio' },
    DEVOLUCION_RECHAZADA:   { level:'ALTO',     badge:'bg-danger',   icon:'bi-x-octagon-fill',       label:'Alto'     },
    COMPRA_CREADA:          { level:'NORMAL',   badge:'bg-info text-dark',    icon:'bi-bag-plus-fill',    label:'Normal'   },
    COMPRA_RECIBIDA:        { level:'NORMAL',   badge:'bg-success',  icon:'bi-box-seam-fill',        label:'Normal'   },
    COMPRA_CANCELADA:       { level:'ALTO',     badge:'bg-danger',   icon:'bi-ban',                  label:'Alto'     },
};

const SEVERITY_ROW = { NORMAL:'', MEDIO:'table-warning', ALTO:'table-danger' };

function severityOf(action) {
    return AUDIT_SEVERITY[action] || { level:'INFO', badge:'bg-secondary', icon:'bi-info-circle', label:'Info' };
}

function searchAuditLogs() { auditPage = 0; loadAuditLogs(); }

function clearAuditFilters() {
    ['audit-from','audit-to','audit-filter-user'].forEach(id => document.getElementById(id).value = '');
    ['audit-filter-entity','audit-filter-action'].forEach(id => document.getElementById(id).value = '');
    auditPage = 0;
    loadAuditLogs();
}

async function loadAuditLogs() {
    const username   = document.getElementById('audit-filter-user')?.value?.trim()  || null;
    const entityType = document.getElementById('audit-filter-entity')?.value         || null;
    const action     = document.getElementById('audit-filter-action')?.value         || null;
    const from       = document.getElementById('audit-from')?.value                  || null;
    const to         = document.getElementById('audit-to')?.value                    || null;

    try {
        const [logs, stats] = await Promise.all([
            api.getAuditLogs(auditPage, 50, username, entityType, action, from, to),
            auditPage === 0 ? api.getAuditStats() : Promise.resolve(null)
        ]);

        auditCurrentLogs = logs;

        if (stats) renderAuditStats(stats);

        const start = auditPage * 50 + 1;
        const end   = auditPage * 50 + logs.length;
        document.getElementById('audit-count-badge').textContent = logs.length;
        document.getElementById('audit-range-info').textContent  = logs.length ? `Registros ${start}–${end}` : '';

        document.getElementById('audit-tbody').innerHTML = logs.length === 0
            ? emptyRow(11, 'Sin registros para los filtros seleccionados')
            : logs.map((l, idx) => {
                const sv  = severityOf(l.action);
                const row = SEVERITY_ROW[sv.level] || '';
                const seq = auditPage * 50 + idx + 1;
                return `<tr class="${row}" style="vertical-align:middle">
                  <td class="text-muted" style="font-size:.75rem;font-family:monospace">${seq}</td>
                  <td class="text-muted" style="font-size:.75rem;white-space:nowrap">${fmtDate(l.timestamp)}</td>
                  <td><span class="badge bg-dark">${l.username}</span></td>
                  <td><span class="badge ${l.userRole==='ADMIN'?'bg-danger':l.userRole==='SUPERVISOR'?'bg-warning text-dark':'bg-primary'}" style="font-size:.65rem">${l.userRole||'—'}</span></td>
                  <td><span class="badge ${sv.badge}" style="font-size:.68rem"><i class="bi ${sv.icon} me-1"></i>${sv.label}</span></td>
                  <td style="font-family:monospace;font-size:.78rem;font-weight:600;letter-spacing:.3px">${l.action}</td>
                  <td><span class="badge bg-light text-dark border" style="font-size:.72rem">${l.entityType||'—'}</span></td>
                  <td class="text-center" style="font-size:.75rem;font-family:monospace">${l.entityId!=null?`#${l.entityId}`:'—'}</td>
                  <td style="font-size:.78rem;max-width:220px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap" title="${(l.details||'').replace(/"/g,'&quot;')}">${l.details||'—'}</td>
                  <td style="font-size:.72rem;font-family:monospace;color:#6c757d">${l.ipAddress||'—'}</td>
                  <td>${l.entityType==='Venta'&&l.entityId
                      ? `<button class="btn btn-xs btn-outline-primary py-0 px-1" onclick="previewSaleFromAudit(${l.entityId})" title="Ver venta"><i class="bi bi-receipt"></i></button>`
                      : ''}</td>
                </tr>`;
              }).join('');

        document.getElementById('audit-page-info').textContent   = `Pág. ${auditPage + 1}`;
        document.getElementById('audit-prev-btn').disabled        = auditPage === 0;
        document.getElementById('audit-next-btn').disabled        = logs.length < 50;
    } catch(e) { toast('Error auditoría: ' + e.message, 'danger'); }
}

function renderAuditStats(stats) {
    document.getElementById('audit-kpi-today').textContent = stats.today  ?? '—';
    document.getElementById('audit-kpi-week').textContent  = stats.week   ?? '—';
    document.getElementById('audit-kpi-month').textContent = stats.month  ?? '—';
    document.getElementById('audit-kpi-total').textContent = stats.total  ?? '—';

    const maxAction = stats.byAction ? Math.max(...Object.values(stats.byAction), 1) : 1;
    document.getElementById('audit-by-action').innerHTML = stats.byAction
        ? Object.entries(stats.byAction).slice(0,7).map(([k,v]) => {
            const sv = severityOf(k);
            const pct = Math.round(v / maxAction * 100);
            return `<li class="list-group-item py-2 px-3" style="font-size:.78rem">
              <div class="d-flex justify-content-between mb-1">
                <span class="badge ${sv.badge}" style="font-size:.65rem">${k}</span>
                <strong>${v}</strong>
              </div>
              <div class="progress" style="height:4px"><div class="progress-bar ${sv.badge.replace('text-dark','')}" style="width:${pct}%"></div></div>
            </li>`;
          }).join('')
        : '<li class="list-group-item text-muted small py-2">Sin datos</li>';

    document.getElementById('audit-by-entity').innerHTML = stats.byEntity
        ? Object.entries(stats.byEntity).map(([k,v]) =>
            `<li class="list-group-item d-flex justify-content-between align-items-center py-2 px-3" style="font-size:.78rem">
              <span>${k}</span><span class="badge bg-secondary rounded-pill">${v}</span>
            </li>`).join('')
        : '<li class="list-group-item text-muted small py-2">Sin datos</li>';

    document.getElementById('audit-top-users').innerHTML = stats.topUsers?.length
        ? stats.topUsers.map((u, i) =>
            `<li class="list-group-item d-flex justify-content-between align-items-center py-2 px-3" style="font-size:.78rem">
              <span><span class="text-muted me-2">${i+1}.</span>${u.username}</span>
              <span class="badge bg-primary rounded-pill">${u.count}</span>
            </li>`).join('')
        : '<li class="list-group-item text-muted small py-2">Sin datos</li>';
}

function loadAuditPage(delta) {
    auditPage = Math.max(0, auditPage + delta);
    loadAuditLogs();
}

function exportAuditExcel() {
    const username   = document.getElementById('audit-filter-user')?.value?.trim()  || '';
    const entityType = document.getElementById('audit-filter-entity')?.value         || '';
    const action     = document.getElementById('audit-filter-action')?.value         || '';
    const from       = document.getElementById('audit-from')?.value                  || '';
    const to         = document.getElementById('audit-to')?.value                    || '';
    toast('Generando informe Excel profesional…', 'info');
    window.location.href = api.getAuditExcelUrl(username, entityType, action, from, to);
}

async function previewSaleFromAudit(saleId) {
    try {
        await viewSale(saleId);
    } catch(e) { toast('No se pudo cargar la venta #' + saleId, 'danger'); }
}

// ── Init ──────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
    document.getElementById('current-date').textContent = new Date().toLocaleDateString('es-CO',{weekday:'long',year:'numeric',month:'long',day:'numeric'});
    try {
        currentUser = await api.me();
        document.getElementById('user-info-sidebar').textContent = currentUser.fullName;
        document.getElementById('user-role-badge').textContent = currentUser.role;
        const navUsername = document.getElementById('navbar-username');
        if (navUsername) navUsername.textContent = currentUser.fullName;
        const roleBadge = document.getElementById('user-role-badge');
        const roleColors = { ADMIN: 'bg-danger', SUPERVISOR: 'bg-warning text-dark', VENDEDOR: 'bg-primary' };
        roleBadge.className = `badge ${roleColors[currentUser.role]||'bg-secondary'}`;
        roleBadge.textContent = currentUser.role;

        document.getElementById('nav-devoluciones').classList.remove('d-none');
        if(currentUser.role === 'ADMIN' || currentUser.role === 'SUPERVISOR') {
            document.getElementById('nav-compras-group').classList.remove('d-none');
        }
        if(currentUser.role === 'ADMIN') {
            document.getElementById('nav-admin-group').classList.remove('d-none');
        }
        // Cargar config empresa en background para tenerla disponible al imprimir
        api.getEmpresa().then(e => { empresaConfig = e; }).catch(()=>{});

        // Aterrizar en la sección apropiada según rol
        const startSection = { ADMIN: 'dashboard', SUPERVISOR: 'cajas', VENDEDOR: 'ventas' };
        const section = startSection[currentUser.role] || 'dashboard';
        const link = document.querySelector(`.sidebar-link[onclick*="'${section}'"]`);
        if (link) showSection(section, link);
        else loadDashboard();
    } catch(e){ loadDashboard(); }
});


function openChangePasswordModal() {
    document.getElementById('cp-current').value = '';
    document.getElementById('cp-new').value = '';
    document.getElementById('cp-confirm').value = '';
    const alert = document.getElementById('cp-alert');
    alert.className = 'd-none';
    alert.textContent = '';
    getModal('modalChangePassword').show();
}

async function doChangePassword() {
    const current = document.getElementById('cp-current').value;
    const newPwd  = document.getElementById('cp-new').value;
    const confirm = document.getElementById('cp-confirm').value;
    const alertEl = document.getElementById('cp-alert');

    const showCpAlert = (msg, type) => {
        alertEl.className = `alert alert-${type} py-2 small`;
        alertEl.textContent = msg;
        alertEl.classList.remove('d-none');
    };

    if (!current) { showCpAlert('Ingresa tu contraseña actual.', 'warning'); return; }
    if (newPwd.length < 6) { showCpAlert('La nueva contraseña debe tener al menos 6 caracteres.', 'warning'); return; }
    if (newPwd !== confirm) { showCpAlert('Las contraseñas no coinciden.', 'warning'); return; }

    try {
        const res = await fetch('/api/users/change-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ currentPassword: current, newPassword: newPwd })
        });
        const data = await res.json();
        if (res.ok) {
            getModal('modalChangePassword').hide();
            toast('Contraseña actualizada correctamente', 'success');
        } else {
            showCpAlert(data.message || 'Error al cambiar la contraseña.', 'danger');
        }
    } catch(e) {
        showCpAlert('Error de conexión.', 'danger');
    }
}

// ── PERFIL DE USUARIO ─────────────────────────────────────────────────
function showProfileTab(tab, el) {
    ['personal','ubicacion','emergencia'].forEach(t =>
        document.getElementById(`ptab-${t}`).classList.toggle('d-none', t !== tab));
    document.querySelectorAll('#profile-tabs .nav-link').forEach(l => l.classList.remove('active'));
    el.classList.add('active');
}

async function openProfileModal() {
    document.getElementById('pf-alert').className = 'd-none';
    document.getElementById('pf-password').value = '';
    // Resetear al primer tab
    showProfileTab('personal', document.querySelector('#profile-tabs .nav-link'));

    try {
        const p = await fetch('/api/users/profile').then(r => r.json());
        document.getElementById('profile-role-label').textContent = p.roleLabel + (p.position ? ' · ' + p.position : '');
        document.getElementById('pf-fullName').value        = p.fullName || '';
        document.getElementById('pf-email').value          = p.email || '';
        document.getElementById('pf-phone').value          = p.phone || '';
        document.getElementById('pf-birthDate').value      = p.birthDate || '';
        document.getElementById('pf-bloodType').value      = p.bloodType || '';
        document.getElementById('pf-position').value       = p.position || '';
        document.getElementById('pf-documentType').value   = p.documentType || '';
        document.getElementById('pf-documentNumber').value = p.documentNumber || '';
        document.getElementById('pf-notes').value          = p.notes || '';
        document.getElementById('pf-address').value        = p.address || '';
        document.getElementById('pf-city').value           = p.city || '';
        document.getElementById('pf-department').value     = p.department || '';
        document.getElementById('pf-emergencyName').value     = p.emergencyContactName || '';
        document.getElementById('pf-emergencyPhone').value    = p.emergencyContactPhone || '';
        document.getElementById('pf-emergencyRelation').value = p.emergencyContactRelation || '';
    } catch(e) { /* campos vacíos si falla */ }

    getModal('modalProfile').show();
}

async function saveProfile() {
    const pwd = document.getElementById('pf-password').value;
    const alertEl = document.getElementById('pf-alert');

    const showPfAlert = (msg, type) => {
        alertEl.className = `alert alert-${type} py-2 small rounded-3`;
        alertEl.innerHTML = msg;
        alertEl.classList.remove('d-none');
    };

    if (!pwd) { showPfAlert('<i class="bi bi-lock me-1"></i>Debes ingresar tu contraseña para guardar los cambios.', 'warning'); return; }

    const btn = document.getElementById('pf-save-btn');
    btn.disabled = true;
    btn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Guardando...';

    const body = {
        fullName:                  document.getElementById('pf-fullName').value,
        email:                     document.getElementById('pf-email').value,
        phone:                     document.getElementById('pf-phone').value,
        birthDate:                 document.getElementById('pf-birthDate').value || null,
        bloodType:                 document.getElementById('pf-bloodType').value,
        position:                  document.getElementById('pf-position').value,
        documentType:              document.getElementById('pf-documentType').value,
        documentNumber:            document.getElementById('pf-documentNumber').value,
        notes:                     document.getElementById('pf-notes').value,
        address:                   document.getElementById('pf-address').value,
        city:                      document.getElementById('pf-city').value,
        department:                document.getElementById('pf-department').value,
        emergencyContactName:      document.getElementById('pf-emergencyName').value,
        emergencyContactPhone:     document.getElementById('pf-emergencyPhone').value,
        emergencyContactRelation:  document.getElementById('pf-emergencyRelation').value,
        currentPassword:           pwd
    };

    try {
        const res = await fetch('/api/users/profile', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });
        const data = await res.json();
        if (res.ok) {
            getModal('modalProfile').hide();
            // Actualizar nombre en navbar si cambió
            if (data.fullName) {
                document.getElementById('navbar-username').textContent = data.fullName;
                document.getElementById('user-info-sidebar').textContent = data.fullName;
            }
            toast('Perfil actualizado correctamente', 'success');
        } else {
            showPfAlert('<i class="bi bi-exclamation-circle me-1"></i>' + (data.message || 'Error al guardar.'), 'danger');
        }
    } catch(e) {
        showPfAlert('Error de conexión.', 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="bi bi-check2-circle me-1"></i>Guardar cambios';
    }
}
