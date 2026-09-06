with open("app/src/main/java/com/focusbyrj/app/FocusApplication.kt", "r") as f:
    content = f.read()

target = """        .createFromAsset("vocab.db")
        .addMigrations(com.focusbyrj.app.data.VocabDatabase.MIGRATION_1_2)
        .build()"""

replacement = """        .createFromAsset("vocab.db")
        .addMigrations(com.focusbyrj.app.data.VocabDatabase.MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()"""

content = content.replace(target, replacement)

with open("app/src/main/java/com/focusbyrj/app/FocusApplication.kt", "w") as f:
    f.write(content)
