with open("app/src/main/java/com/focusbyrj/app/ui/components/DuolingoMysteryChestDialog.kt", "r") as f:
    content = f.read()

content = content.replace("MysteryReward(xp = 1000, gold = 10000, streakFreezeAwarded = false)", "MysteryReward(xp = 0, gold = 50, streakFreezeAwarded = false)")

with open("app/src/main/java/com/focusbyrj/app/ui/components/DuolingoMysteryChestDialog.kt", "w") as f:
    f.write(content)
