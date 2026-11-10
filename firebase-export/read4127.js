const fs = require('fs');

const content = fs.readFileSync('C:\\Users\\Abuzar\\.gemini\\antigravity\\brain\\da218d80-77f1-49cf-9793-dab4e2102618\\.system_generated\\logs\\transcript_full.jsonl', 'utf8');
const lines = content.split('\n');
for (const line of lines) {
  if (line.includes('"step_index":4127')) {
    try {
      const obj = JSON.parse(line);
      console.log("=== STEP 4127 ===");
      console.log(JSON.stringify(obj.tool_calls || obj.content, null, 2));
    } catch (e) {
      console.log(e.message);
    }
  }
}
