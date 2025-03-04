const fetch = require('node-fetch');
const fs = require('fs');
const path = require('path');

// Function to download a single file
async function downloadFile(fileId, pattern, extension) {
    const url = `https://toto.dola/api/storage/substorage/${fileId}/download`;
    
    // Construct the filename using the provided pattern and extension
    const fileName = `${pattern}_${fileId}.${extension}`;
    const filePath = path.join(__dirname, fileName);  // Save in current directory
    
    try {
        // Fetch the file from the URL
        const response = await fetch(url);

        if (response.ok) {
            // Convert the response into a buffer (binary data)
            const buffer = await response.buffer();
            
            // Write the file to disk
            fs.writeFileSync(filePath, buffer);
            console.log(`Downloaded ${fileName} to ${filePath}`);
        } else {
            console.log(`Failed to download ${fileId}: ${response.status}`);
        }
    } catch (error) {
        console.log(`Error downloading ${fileId}: ${error.message}`);
    }
}

// Function to download multiple files
async function downloadFiles(fileIds, pattern, extension) {
    for (const fileId of fileIds) {
        await downloadFile(fileId, pattern, extension);
    }
}

// Example usage: Provide the list of file IDs, desired pattern, and extension
const fileIds = ["12345", "67890", "abcde"];  // Replace with your actual file IDs
const pattern = "customPattern";  // Input the desired pattern
const extension = "csv";  // Input the desired extension (e.g., csv, txt, pdf, etc.)

// Start downloading the files
downloadFiles(fileIds, pattern, extension);
