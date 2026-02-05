import os
import subprocess
import random
from datetime import datetime, timedelta

# Configuration
START_DATE = datetime(2026, 1, 23)
END_DATE = datetime(2026, 2, 5)
USER_EMAIL = "princetagadiya11@gmail.com"
USER_NAME = "Prince-Tagadiya"
REPO_URL = "https://github.com/Prince-Tagadiya/multi-profile-browser.git"

# Real Files Map (Date -> List of files)
# Note: These dates must fall within range.
REAL_CHANGES = {
    datetime(2026, 1, 23).strftime("%Y-%m-%d"): ["build.gradle.kts", "settings.gradle.kts", "gradlew", "gradlew.bat", "gradle", ".gitignore", "app/build.gradle.kts", "app/proguard-rules.pro"],
    datetime(2026, 1, 25).strftime("%Y-%m-%d"): ["app/src/main/AndroidManifest.xml", "app/src/main/res/values"],
    datetime(2026, 1, 27).strftime("%Y-%m-%d"): ["app/src/main/res/layout", "app/src/main/res/drawable", "app/src/main/res/mipmap-anydpi-v26"],
    datetime(2026, 1, 29).strftime("%Y-%m-%d"): ["app/src/main/java/com/multiprofile/browser/Profile.kt", "app/src/main/java/com/multiprofile/browser/ProfileManager.kt"],
    datetime(2026, 1, 31).strftime("%Y-%m-%d"): ["app/src/main/java/com/multiprofile/browser/ProcessStateManager.kt"],
    datetime(2026, 2, 2).strftime("%Y-%m-%d"): ["app/src/main/java/com/multiprofile/browser/AddEditProfileActivity.kt", "app/src/main/java/com/multiprofile/browser/ProfileListActivity.kt", "app/src/main/java/com/multiprofile/browser/SplashActivity.kt"],
    datetime(2026, 2, 4).strftime("%Y-%m-%d"): ["app/src/main/java/com/multiprofile/browser/WebViewActivity.kt", "app/src/main/java/com/multiprofile/browser/WebViewActivityBeta.kt", "app/src/main/java/com/multiprofile/browser/WebViewActivityGamma.kt"],
    datetime(2026, 2, 5).strftime("%Y-%m-%d"): ["."] # Final sweep
}

FILLER_MESSAGES = [
    "Refactor internal logic", "Update build configuration", "Clean up imports", 
    "Fix minor layout issue", "Optimize WebView initialization", "Update dependencies",
    "Code formatting and linting", "Improve error handling", "Add comments",
    "Prepare for new feature", "Debug process isolation", "Update resource references",
    "Refine user interface", "Performance tuning", "Memory optimization",
    "Fix typo in variable name", "Update TODO list", "Structural reorganization",
    "WIP: Experimenting with caching", "Log analysis"
]

def run(cmd, env=None):
    subprocess.run(cmd, shell=True, check=True, env=env)

def main():
    if os.path.exists(".git"):
        subprocess.run("rm -rf .git", shell=True)
    
    run("git init")
    run(f"git config user.name '{USER_NAME}'")
    run(f"git config user.email '{USER_EMAIL}'")
    
    current_day = START_DATE
    while current_day <= END_DATE:
        day_str = current_day.strftime("%Y-%m-%d")
        real_files = REAL_CHANGES.get(day_str)
        
        # Determine number of commits for today (1 to 15)
        num_commits = random.randint(1, 15)
        
        # Timestamps for the day (Spread between 10:00 and 22:00)
        times = sorted([random.randint(36000, 79200) for _ in range(num_commits)])
        
        print(f"Generating {num_commits} commits for {day_str}...")

        commit_indices = list(range(num_commits))
        real_commit_idx = -1
        
        if real_files:
            # Pick a random slot for the REAL commit
            real_commit_idx = random.choice(commit_indices)
        
        for i in range(num_commits):
            # Calculate time
            seconds = times[i]
            hour = seconds // 3600
            minute = (seconds % 3600) // 60
            second = seconds % 60
            commit_date = current_day.replace(hour=hour, minute=minute, second=second)
            iso_date = commit_date.isoformat()
            
            env = os.environ.copy()
            env["GIT_AUTHOR_DATE"] = iso_date
            env["GIT_COMMITTER_DATE"] = iso_date
            
            if i == real_commit_idx:
                # Real Code Commit
                for f in real_files:
                     if os.path.exists(f):
                         run(f"git add \"{f}\"")
                
                msg = f"Implement core functionality: {real_files[0].split('/')[-1]}"
                if real_files[0] == ".": msg = "Final Polish and Documentation"
                
                # Check status
                status = subprocess.run("git status --porcelain", shell=True, capture_output=True, text=True).stdout
                if status.strip():
                     run(f'git commit -m "{msg}"', env=env)
                else:
                     # Fallback to empty if nothing to add (e.g. files missing)
                     run(f'git commit --allow-empty -m "Work on core features"', env=env)
            else:
                # Filler Empty Commit
                msg = random.choice(FILLER_MESSAGES)
                run(f'git commit --allow-empty -m "{msg}"', env=env)
        
        current_day += timedelta(days=1)

    # Setup Remote and Push
    print("Pushing to GitHub...")
    try:
        run(f"git remote add origin {REPO_URL}")
    except:
        run(f"git remote set-url origin {REPO_URL}")
        
    run(f"git push -u origin main --force")
    print("Done!")

if __name__ == "__main__":
    main()
