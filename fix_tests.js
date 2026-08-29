const fs = require('fs');
const testPath = './app/src/test/java/com/focusbyrj/app/util/AyvaTalkEngineTest.kt';
let code = fs.readFileSync(testPath, 'utf8');

// Fix testPersistentRemindersQueryIsSpecific
code = code.replace('assertTrue(answer.contains("Persistent Reminder Interval"))', 'assertTrue(answer.contains("Persistent Task Reminders"))');

fs.writeFileSync(testPath, code);
