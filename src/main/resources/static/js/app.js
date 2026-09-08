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

        // Close the mobile on-screen keyboard after every scan, then silently
        // restore focus (without reopening the keyboard) so the field stays
        // ready for the next scan. See hideKeyboardAndRefocus() below.
        hideKeyboardAndRefocus(input);
    }
}

/**
 * Hides the mobile virtual keyboard after a scan is processed.
 *
 * Why this is needed:
 * On mobile browsers, a focused text input always opens the on-screen
 * keyboard, which can cover half the screen. Simply calling blur() closes
 * the keyboard but also removes focus, which breaks continuous scanning
 * with hardware/Bluetooth barcode scanners (they type into whichever field
 * currently has focus).
 *
 * How it works:
 * 1. blur() immediately removes focus, which closes the keyboard.
 * 2. After a short delay, the input is temporarily marked "readonly".
 * 3. Focusing a "readonly" input does NOT trigger the mobile keyboard.
 * 4. The "readonly" attribute is removed right after, so normal typing
 *    and hardware-scanner input continue to work exactly as before.
 *
 * Net result: keyboard stays hidden, but the field is still focused and
 * ready to receive the next scanned LPN.
 *
 * @param {HTMLInputElement} input - the LPN input field
 */
function hideKeyboardAndRefocus(input) {
    input.blur();

    setTimeout(() => {
        input.setAttribute("readonly", "readonly");
        input.focus();

        // Remove readonly right after focus so scanning/typing works normally
        setTimeout(() => input.removeAttribute("readonly"), 50);
    }, 150);
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

    // Direct binding with Spring Boot LpnResponseDTO field names
    const lpn = data.lpn || '';
    const group = data.ptlGroup || '';
    const location = data.location || ''; // Correctly mapped with LpnResponseDTO.java

    // Extract zone pairs array (up to 16 pairs)
    let zones = [];
    if (data.ptlMergedGroups) {
        zones = Array.isArray(data.ptlMergedGroups)
            ? data.ptlMergedGroups
            : data.ptlMergedGroups.toString().split(/,\s*|\s+/);
    }

    // 4x4 Grid Boxes layout parameters
    const boxWidth = 110;
    const boxHeight = 26;
    const boxGapX = 15;
    const startX = 35;
    const startY = 185;
    const rowGapY = 34;

    let zonesTspl = "";
    for (let i = 0; i < Math.min(zones.length, 16); i++) {
        let row = Math.floor(i / 4);
        let col = i % 4;

        let x1 = startX + col * (boxWidth + boxGapX);
        let y1 = startY + row * rowGapY;
        let x2 = x1 + boxWidth;
        let y2 = y1 + boxHeight;

        // Center zone text inside box
        let textX = x1 + Math.floor(boxWidth / 2);
        let textY = y1 + 5;

        zonesTspl += `BOX ${x1},${y1},${x2},${y2},2\n`;
        zonesTspl += `TEXT ${textX},${textY},"2",0,1,1,2,"${zones[i]}"\n`;
    }

    // Format print Date and Time
    const now = new Date();
    const dateTimeStr = now.toLocaleDateString('en-GB') + '  ' + now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit', hour12: true });

    const tspl = `
SIZE 70 mm, 48 mm
GAP 2 mm, 0
DIRECTION 0
CLS

BOX 20,10,540,370,3

TEXT 280,18,"3",0,1,1,2,"V-MART PALWAL"
BAR 20,45,520,2

TEXT 35,62,"3",0,1,1,"LPN:"
BARCODE 120,52,"128",32,1,0,2,2,"${lpn}"

BAR 20,110,520,2

TEXT 35,120,"3",0,1,1,"GRP: ${group}"
TEXT 320,120,"3",0,1,1,"LOC: ${location}"

BAR 20,150,520,2

TEXT 35,160,"2",0,1,1,"ZONES:"

${zonesTspl}

BAR 20,332,520,2
TEXT 35,342,"2",0,1,1,"Printed On:  ${dateTimeStr}"

PRINT 1,1
`;

    try {
        await globalPrinterWriter.write(new TextEncoder().encode(tspl));
        notify("Label Sent to Printer", "success");
    } catch (e) {
        notify("Printing Failed: " + e.message, "error");
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