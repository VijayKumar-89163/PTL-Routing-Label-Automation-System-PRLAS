let currentPage = 0;
const PAGE_SIZE = 10;
let totalPages = 1;

function notify(msg, type = 'info') {
    let bg = type === 'success' ? '#198754' : type === 'error' ? '#dc3545' : '#0dcaf0';
    if (typeof Toastify !== 'undefined') {
        Toastify({ text: msg, duration: 3000, style: { background: bg, borderRadius: '6px' } }).showToast();
    } else {
        alert(msg);
    }
}

document.addEventListener("DOMContentLoaded", () => {
    fetchScanHistory();
});

// Builds the shared filter query string (search, date range, status) used
// by BOTH the table fetch and the export calls, so they always match.
function buildFilterParams() {
    const params = new URLSearchParams();

    const search = document.getElementById("searchInput")?.value.trim();
    const fromDate = document.getElementById("fromDateInput")?.value;
    const toDate = document.getElementById("toDateInput")?.value;
    const status = document.getElementById("statusSelect")?.value;

    if (search) params.append("search", search);
    if (fromDate) params.append("fromDate", fromDate + ":00");
    if (toDate) params.append("toDate", toDate + ":00");
    if (status) params.append("status", status);

    return params;
}

function applyFilters() {
    currentPage = 0;
    fetchScanHistory();
}

function changePage(direction) {
    const newPage = currentPage + direction;
    if (newPage < 0 || newPage >= totalPages) return;
    currentPage = newPage;
    fetchScanHistory();
}

async function fetchScanHistory() {
    const params = buildFilterParams();
    params.append("page", currentPage);
    params.append("size", PAGE_SIZE);

    try {
        const res = await fetch(`/api/v1/scan-history?${params.toString()}`);
        const data = await res.json();

        if (!res.ok) {
            notify(data.message || "Failed to load scan history", "error");
            return;
        }

        renderTable(data.content || []);
        totalPages = data.totalPages || 1;
        updatePaginationUI(data);

    } catch (e) {
        notify("Backend Connection Failure", "error");
        console.error(e);
    }
}

function renderTable(records) {
    const tbody = document.getElementById("scanHistoryBody");
    if (!tbody) return;

    if (!records || records.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center py-3 text-muted">No records found</td></tr>`;
        return;
    }

    tbody.innerHTML = records.map(r => {
        const statusBadge = r.status === 'FOUND'
            ? '<span class="badge bg-success-subtle text-success">FOUND</span>'
            : '<span class="badge bg-danger-subtle text-danger">NOT FOUND</span>';

        return `
            <tr>
                <td class="fw-bold">${r.lpn}</td>
                <td><span class="badge bg-secondary">${r.ptlGroup || '--'}</span></td>
                <td>${r.location || '--'}</td>
                <td><small class="fw-semibold text-truncate d-inline-block" style="max-width: 250px;">${r.ptlMergedGroups || '--'}</small></td>
                <td>${statusBadge}</td>
                <td><small class="text-muted">${new Date(r.scannedAt).toLocaleString()}</small></td>
            </tr>
        `;
    }).join("");
}

function updatePaginationUI(pageData) {
    const pageInfo = document.getElementById("pageInfoText");
    const prevBtn = document.getElementById("prevPageBtn");
    const nextBtn = document.getElementById("nextPageBtn");

    if (pageInfo) {
        pageInfo.innerText = `Page ${currentPage + 1} of ${pageData.totalPages || 1} (${pageData.totalElements || 0} records)`;
    }
    if (prevBtn) prevBtn.disabled = currentPage === 0;
    if (nextBtn) nextBtn.disabled = currentPage >= (pageData.totalPages || 1) - 1;
}

// Downloads the CSV/Excel export using the SAME filters currently applied on screen
function exportScanHistory(format) {
    const params = buildFilterParams();
    params.append("format", format);

    // Simple navigation triggers the browser's native file download
    // (Content-Disposition: attachment header from the backend handles the rest)
    window.location.href = `/api/v1/scan-history/export?${params.toString()}`;
}
