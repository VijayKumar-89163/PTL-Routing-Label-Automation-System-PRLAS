/**
 * V-Mart PRLAS Shared Front-End Engine
 * Features: Auto-Print, Web-Serial Thermal Printing, Zone Formatting & History Rendering
 */

let globalPrinterPort = null;
let globalPrinterWriter = null;

function notify(msg, type = 'info') {
    let bg = type === 'success' ? '#198754' : type === 'error' ? '#dc3545' : '#0dcaf0';
    Toastify({ text: msg, duration: 3000, style: { background: bg, borderRadius: '6px' } }).showToast();
}

document.addEventListener("DOMContentLoaded", () => {
    fetchRecentScans();
});

// Process Scan and Auto-Print if enabled
async function processLpnScan() {
    const input = document.getElementById("lpnInput");
    if (!input || !input.value.trim()) return;

    const lpn = input.value.trim();

    try {
        const response = await fetch(`/api/lpn/scan?lpn=${encodeURIComponent(lpn)}`);
        const data = await response.json();

        if (response.ok) {
            notify(`LPN Found: ${data.lpn}`, "success");
            renderScanResult(data);
            fetchRecentScans();

            // Auto-Print Trigger
            const isAutoPrint = document.getElementById("autoPrintCheck")?.checked;
            if (isAutoPrint) {
                printTsplLabel(data);
            }
        } else {
            notify(data.message || "LPN NOT FOUND", "error");
        }
    } catch (e) {
        notify("Backend Connection Failure", "error");
    } finally {
        input.value = "";
        input.focus();
    }
}

// Render 16 Zones (8 Pairs Layout) inside Screen UI
function renderScanResult(data) {
    const card = document.getElementById("resultCard");
    card.style.display = "block";

    // Split zones string into pairs
    const pairs = data.ptlMergedGroups ? data.ptlMergedGroups.split(",") : [];

    let zonesHtml = '<div class="row g-2 mt-2">';
    pairs.forEach(p => {
        zonesHtml += `<div class="col-6 col-md-3"><div class="zone-pair-badge">${p.trim()}</div></div>`;
    });
    zonesHtml += '</div>';

    card.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-2 border-bottom pb-2">
            <h6 class="fw-bold m-0 text-success"><i class="bi bi-check-circle-fill me-1"></i> LPN MATCHED</h6>
            <button class="btn btn-sm btn-vm-primary" onclick='printTsplLabel(${JSON.stringify(data)})'><i class="bi bi-printer me-1"></i> Print Label</button>
        </div>
        <div class="row g-2">
            <div class="col-4"><strong>LPN:</strong> ${data.lpn}</div>
            <div class="col-4"><strong>Group:</strong> ${data.ptlGroup}</div>
            <div class="col-4"><strong>Location:</strong> ${data.location}</div>
        </div>
        <div class="mt-2"><small class="fw-bold text-muted">16 ZONES (8 PAIRS GRID):</small>${zonesHtml}</div>
    `;
}

// Fetch Top 10 Recent Scans
async function fetchRecentScans() {
    try {
        const res = await fetch("/api/lpn/recent-scans");
        const list = await res.json();
        const tbody = document.getElementById("recentScansBody");
        if (!tbody) return;

        if (!list || list.length === 0) {
            tbody.innerHTML = `<tr><td colspan="5" class="text-center py-3 text-muted">No records</td></tr>`;
            return;
        }

        tbody.innerHTML = list.slice(0, 10).map(s => `
            <tr>
                <td class="fw-bold">${s.lpn}</td>
                <td><span class="badge bg-secondary">${s.ptlGroup}</span></td>
                <td>${s.location}</td>
                <td><small class="fw-semibold text-truncate d-inline-block" style="max-width: 300px;">${s.ptlMergedGroups}</small></td>
                <td><small class="text-muted">${new Date(s.scannedAt).toLocaleTimeString()}</small></td>
            </tr>
        `).join("");
    } catch (e) {
        console.error("Scan fetch error:", e);
    }
}

// Connect Thermal Printer via Web Serial
async function connectPrinter() {
    if (!("serial" in navigator)) {
        notify("Browser does not support Web Serial API", "error");
        return;
    }
    try {
        globalPrinterPort = await navigator.serial.requestPort();
        await globalPrinterPort.open({ baudRate: 9600 });
        globalPrinterWriter = globalPrinterPort.writable.getWriter();

        const statusText = document.getElementById("printerStatusText");
        if(statusText) {
            statusText.innerText = "CONNECTED";
            statusText.className = "m-0 fw-bold text-success";
        }
        notify("Thermal Printer Ready!", "success");
    } catch (err) {
        notify("Printer Connection Cancelled", "error");
    }
}

// TSPL Printing Engine with 16 Zones Formatting
async function printTsplLabel(data) {
    if (!globalPrinterWriter) {
        notify("Thermal Printer Not Connected!", "error");
        return;
    }

    const tspl = `
SIZE 70 mm, 48 mm
GAP 2 mm, 0
CLS
TEXT 30,15,"3",0,1,1,"V-MART LOGISTICS"
TEXT 30,40,"2",0,1,1,"LPN: ${data.lpn}"
BARCODE 30,65,"128",35,1,0,2,2,"${data.lpn}"
TEXT 30,110,"2",0,1,1,"GRP: ${data.ptlGroup} | LOC: ${data.location}"
TEXT 30,135,"2",0,1,1,"ZONES: ${data.ptlMergedGroups}"
PRINT 1,1
`;

    try {
        await globalPrinterWriter.write(new TextEncoder().encode(tspl));
        notify("Label Sent to Printer", "success");
    } catch (e) {
        notify("Printing Failed", "error");
    }
}

// Handle Modal CSV Upload Trigger
async function uploadCsvFromModal() {
    const fileInput = document.getElementById("modalCsvInput");
    if (!fileInput.files[0]) {
        notify("Select a CSV file first", "error");
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    try {
        const res = await fetch("/api/lpn/upload-csv", { method: "POST", body: formData });
        const data = await res.json();
        if (res.ok) {
            notify(`Success! ${data.totalRows} Rows Uploaded.`, "success");
            fetchRecentScans();
            bootstrap.Modal.getInstance(document.getElementById('csvUploadModal')).hide();
        } else {
            notify(data.message, "error");
        }
    } catch (e) {
        notify("CSV Upload Exception", "error");
    }
}