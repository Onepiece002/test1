logcat -d > log.txt
grep -E "FATAL|Exception|Error" log.txt | tail -n 30
