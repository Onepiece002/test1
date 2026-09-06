with open("app/src/main/java/com/focusbyrj/app/data/VocabDao.kt", "r") as f:
    content = f.read()

content = content.replace("ORDER BY repetition_ssc DESC LIMIT :limit", "ORDER BY is_top_200 DESC, repetition_ssc DESC LIMIT :limit")

with open("app/src/main/java/com/focusbyrj/app/data/VocabDao.kt", "w") as f:
    f.write(content)
