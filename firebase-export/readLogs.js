const fs = require('fs');

const content = fs.readFileSync('C:\\Users\\Abuzar\\.gemini\\antigravity\\brain\\da218d80-77f1-49cf-9793-dab4e2102618\\.system_generated\\logs\\transcript_full.jsonl', 'utf8');
const lines = content.split('\n');
for (const line of lines) {
  if (line.includes('"step_index":3464') || line.includes('"step_index":3465')) {
    try {
      const obj = JSON.parse(line);
      console.log(`=== STEP ${obj.step_index} ===`);
      console.log(obj.content);
    } catch (e) {
      console.log("Error parsing:", e.message);
    }
  }
}
