async function downloadFile(fileId, pattern, extension) {
    const url = `https://toto.dola/api/storage/substorage/${fileId}/download`;

    // Construct the filename using the provided pattern and extension
    const fileName = `${pattern}_${fileId}.${extension}`;  

    const response = await fetch(url);

    if (response.ok) {
        const blob = await response.blob();
        const a = document.createElement("a");
        const downloadUrl = window.URL.createObjectURL(blob);
        a.href = downloadUrl;
        a.download = fileName;  // Use the custom filename
        document.body.appendChild(a);
        a.click();
        a.remove();
        console.log(`Downloaded ${fileName}`);
    } else {
        console.log(`Failed to download ${fileId}: ${response.status}`);
    }
}

async function downloadFiles(fileIds, pattern, extension) {
    for (const fileId of fileIds) {
        await downloadFile(fileId, pattern, extension);
    }
}

// List of file IDs
const fileIds = ["12345", "67890", "abcde"]; // Replace with your actual file IDs

// Pattern and extension input
const pattern = "customPattern";  // Input the desired pattern
const extension = "csv";          // Input the desired extension (e.g., csv, txt, pdf, etc.)

downloadFiles(fileIds, pattern, extension);
