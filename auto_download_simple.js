async function downloadFile(fileId) {
    const url = `https://toto.dola/api/storage/substorage/${fileId}/download`;
    const fileName = `pattern_${fileId}.csv`;
    const response = await fetch(url);

    if (response.ok) {
        const blob = await response.blob();
        const a = document.createElement("a");
        const url = window.URL.createObjectURL(blob);
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        a.remove();
        console.log(`Downloaded ${fileName}`);
    } else {
        console.log(`Failed to download ${fileId}: ${response.status}`);
    }
}

async function downloadFiles(fileIds) {
    for (const fileId of fileIds) {
        await downloadFile(fileId);
    }
}

// List of file IDs
const fileIds = ["12345", "67890", "abcde"]; // Replace with your actual file IDs
downloadFiles(fileIds);
