const fs = require('fs');

const content = fs.readFileSync('C:\\Users\\Abuzar\\.gemini\\antigravity\\brain\\da218d80-77f1-49cf-9793-dab4e2102618\\.system_generated\\logs\\transcript_full.jsonl', 'utf8');
const lines = content.split('\n');
for (const line of lines) {
  if (line.includes('"step_index":4128')) {
    const obj = JSON.parse(line);
    fs.writeFileSync('d:\\antigravity project\\Autocontactssaver2\\firebase-export\\step4128_full.txt', obj.content);
    console.log("Written step4128_full.txt, length:", obj.content.length);
  }
}
