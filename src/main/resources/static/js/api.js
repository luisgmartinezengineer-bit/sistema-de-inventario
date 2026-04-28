const BASE = '/api';

async function req(method, path, body = null) {
    const opts = { method, headers: { 'Content-Type': 'application/json' } };
    if (body) opts.body = JSON.stringify(body);
    const res = await fetch(BASE + path, opts);
    if (res.status === 401) { window.location.href = '/login'; return; }
    if (!res.ok) {
        const err = await res.json().catch(() => ({}));
        throw new Error(err.message || `Error ${res.status}`);
    }
    if (res.status === 204) return null;
    return res.json();
}

const api = {
    // Auth
    me:              ()        => req('GET',    '/auth/me'),

    // Categorías
    getCategories:   ()        => req('GET',    '/categories'),
    createCategory:  (d)       => req('POST',   '/categories', d),
    updateCategory:  (id, d)   => req('PUT',    `/categories/${id}`, d),
    deleteCategory:  (id)      => req('DELETE', `/categories/${id}`),

    // Productos
    getProducts:     ()        => req('GET',    '/products'),
    getLowStock:     ()        => req('GET',    '/products/low-stock'),
    createProduct:   (d)       => req('POST',   '/products', d),
    updateProduct:   (id, d)   => req('PUT',    `/products/${id}`, d),
    deleteProduct:   (id)      => req('DELETE', `/products/${id}`),
    adjustStock:     (id, d)   => req('PATCH',  `/products/${id}/stock`, d),

    // Clientes
    getCustomers:    ()        => req('GET',    '/customers'),
    searchCustomers: (q)       => req('GET',    `/customers?search=${q}`),
    createCustomer:  (d)       => req('POST',   '/customers', d),
    updateCustomer:  (id, d)   => req('PUT',    `/customers/${id}`, d),
    deleteCustomer:  (id)      => req('DELETE', `/customers/${id}`),

    // Ventas
    getSales:           ()        => req('GET',    '/sales'),
    getSalesByCaja:     (id)      => req('GET',    `/sales?cashRegisterId=${id}`),
    getSalesByInvoice:  (q)       => req('GET',    `/sales?invoice=${encodeURIComponent(q)}`),
    getSale:            (id)      => req('GET',    `/sales/${id}`),
    getSummary:         ()        => req('GET',    '/sales/summary'),
    createSale:         (d)       => req('POST',   '/sales', d),

    // Devoluciones
    getReturns:         ()        => req('GET',    '/returns'),
    getReturnsPending:  ()        => req('GET',    '/returns/pending'),
    getMyReturns:       ()        => req('GET',    '/returns/my'),
    getReturn:          (id)      => req('GET',    `/returns/${id}`),
    getReturnsBySale:   (saleId)  => req('GET',    `/returns/by-sale/${saleId}`),
    createReturn:       (d)       => req('POST',   '/returns', d),
    approveReturn:      (id)      => req('PATCH',  `/returns/${id}/approve`),
    rejectReturn:       (id, d)   => req('PATCH',  `/returns/${id}/reject`, d),

    // Cajas
    getCajas:        ()        => req('GET',    '/cajas'),
    getCajasOpen:    ()        => req('GET',    '/cajas/open'),
    openCaja:        (d)       => req('POST',   '/cajas', d),
    closeCaja:       (id, d)   => req('PATCH',  `/cajas/${id}/close`, d),

    // Usuarios (solo ADMIN)
    getUsers:        ()        => req('GET',    '/users'),
    createUser:      (d)       => req('POST',   '/users', d),
    updateUser:      (id, d)   => req('PUT',    `/users/${id}`, d),
    toggleUser:      (id)      => req('PATCH',  `/users/${id}/toggle`),

    // Alertas
    getActiveAlerts: ()        => req('GET',    '/alerts/active'),
    resolveAlert:    (id)      => req('PATCH',  `/alerts/${id}/resolve`),

    // Empresa
    getEmpresa:      ()        => req('GET',    '/empresa'),
    saveEmpresa:     (d)       => req('POST',   '/empresa', d),

    // Reportes contables
    getAccounting:   (from, to, cajaId) => {
        let url = `/reports/accounting?from=${from}&to=${to}`;
        if (cajaId) url += `&cashRegisterId=${cajaId}`;
        return req('GET', url);
    },

    // Compras
    getPurchases:        ()        => req('GET',    '/purchases'),
    getPurchase:         (id)      => req('GET',    `/purchases/${id}`),
    createPurchase:      (d)       => req('POST',   '/purchases', d),
    receivePurchase:     (id)      => req('PATCH',  `/purchases/${id}/receive`),
    cancelPurchase:      (id)      => req('PATCH',  `/purchases/${id}/cancel`),

    // Analítica
    getAnalytics:        (days)    => req('GET',    `/analytics?days=${days||30}`),

    // Auditoría
    getAuditLogs: (page, size, username, entityType, action, from, to) => {
        let url = `/audit?page=${page||0}&size=${size||50}`;
        if (username)   url += `&username=${encodeURIComponent(username)}`;
        if (entityType) url += `&entityType=${encodeURIComponent(entityType)}`;
        if (action)     url += `&action=${encodeURIComponent(action)}`;
        if (from)       url += `&from=${from}`;
        if (to)         url += `&to=${to}`;
        return req('GET', url);
    },
    getAuditStats: () => req('GET', '/audit/stats'),
    getAuditExcelUrl: (username, entityType, action, from, to) => {
        let url = `/api/audit/export/excel?_=1`;
        if (username)   url += `&username=${encodeURIComponent(username)}`;
        if (entityType) url += `&entityType=${encodeURIComponent(entityType)}`;
        if (action)     url += `&action=${encodeURIComponent(action)}`;
        if (from)       url += `&from=${from}`;
        if (to)         url += `&to=${to}`;
        return url;
    },

    // Productos con vencimiento
    getExpiringProducts: (days)    => req('GET',    `/products/expiring?days=${days||30}`),

    // Proveedores
    getSupplierDashboard:      ()           => req('GET',    '/suppliers/dashboard'),
    getSuppliers:              ()           => req('GET',    '/suppliers'),
    getSupplier:               (id)         => req('GET',    `/suppliers/${id}`),
    createSupplier:            (d)          => req('POST',   '/suppliers', d),
    updateSupplier:            (id, d)      => req('PUT',    `/suppliers/${id}`, d),
    deleteSupplier:            (id)         => req('DELETE', `/suppliers/${id}`),

    getSupplierProducts:       (supplierId) => req('GET',    `/suppliers/${supplierId}/products`),
    getSuppliersByProduct:     (productId)  => req('GET',    `/suppliers/by-product/${productId}`),
    addSupplierProduct:        (d)          => req('POST',   '/suppliers/products', d),
    deleteSupplierProduct:     (spId)       => req('DELETE', `/suppliers/products/${spId}`),

    getQuotes:                 (spId)       => req('GET',    `/suppliers/products/${spId}/quotes`),
    addQuote:                  (spId, d)    => req('POST',   `/suppliers/products/${spId}/quotes`, d),

    analyzePrices:             (spId)       => req('GET',    `/suppliers/products/${spId}/analysis`),
    compareSuppliers:          (productId)  => req('GET',    `/suppliers/compare/${productId}`),

    getSupplierEvents:         ()           => req('GET',    '/suppliers/events'),
    getUpcomingEvents:         ()           => req('GET',    '/suppliers/events/upcoming'),
    createSupplierEvent:       (d)          => req('POST',   '/suppliers/events', d),

    getSupplierAlerts:         ()           => req('GET',    '/suppliers/alerts'),
    getAllSupplierAlerts:       ()           => req('GET',    '/suppliers/alerts/all'),
    resolveSupplierAlert:      (id)         => req('PATCH',  `/suppliers/alerts/${id}/resolve`),
};
