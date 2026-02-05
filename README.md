# Profile Browser

**Profile Browser** is an advanced Android application designed to enable simultaneous, isolated browsing sessions for the same website. By leveraging Android's multi-process architecture, it allows users to maintain distinct login sessions for different accounts (e.g., multiple Seller accounts, social media profiles) without the need for constant logging in and out.

> **Status**: 🚧 Actively Under Development

---

## 🚀 Overview

Many users manage multiple accounts for the same service (like Amazon Seller, Meesho, or Gmail). Standard browsers force you to use Incognito mode (which doesn't save sessions) or constantly switch accounts.

**Profile Browser** solves this by treating every "Profile" as a completely isolated container with its own cookies, cache, and storage. It combines the utility of multiple browser profiles with the convenience of a modern, tabbed mobile interface.

---

## ✨ Key Features

*   **True Isolation**: Each profile runs in its own data directory suffix, ensuring zero data leakage between accounts.
*   **Persistent Sessions**: Cookies and logins are saved per profile. Close the app and reopen it later—you are still logged in.
*   **Smart Tab Switching**: A custom "Smart Switch" architecture keeps the most recent profiles active in memory for instant switching, while aggressively cleaning up distant profiles to save battery and RAM.
*   **Performance Focused**: Implements a dedicated 3-process caching strategy (Main, Beta, Gamma) to handle heavy WebViews efficiently.
*   **Clean UI**: A clutter-free, material design inspired interface that puts your content first.

---

## 📅 Development Timeline

This project has been a focused engineering effort started in late January.

### **Phase 1: Concept & Architecture (Jan 23rd – Jan 26th)**
*   Researched constraints of `Android WebView` regarding cookie isolation.
*   Identified `WebView.setDataDirectorySuffix()` as the core solution.
*   Designed the multi-process architecture (`:webview`, `:webview_beta`, etc.) to overcome Android's single-process limitation for WebView data directories.

### **Phase 2: Core Implementation (Jan 27th – Feb 1st)**
*   Built the fundamental `WebViewActivity` capable of handling dynamic profile IDs.
*   Implemented `ProcessStateManager` to track which profile is loaded in which OS process.
*   Established the basic data persistence layer for storing profile metadata.

### **Phase 3: UX & Optimization (Feb 2nd – Feb 6th)**
*   **UI Overhaul**: Redesigned the main interface to resemble a modern browser with a scrollable tab bar.
*   **Smart Caching**: Implemented a "Sliding Window" cache (Previous, Current, Next) to make checking other tabs feel instant.
*   **Stability**: Fixed critical crash loops related to process reuse and dirty state handling (Self-healing process logic).
*   **Refinement**: Added visual loading indicators per tab and "Active Tab" auto-scrolling.

### **Current Focus (Ongoing)**
*   Further reducing memory footprint.
*   Enhancing the "Add Profile" workflow with more quick-shortcuts.
*   Preparing for v1.0 release.

---

## 🛠 Tech Stack

*   **Language**: Kotlin
*   **Core Component**: Android WebView (Multi-Process)
*   **UI Toolkit**: XML Layouts / Material Design Components
*   **Architecture**: Android Activity-based Process Isolation
*   **Minimum SDK**: Android 9 (API 28)+

---

## 👨‍💻 Developer

Built and maintained by **Prince Nareshbhai Tagadiya**.

*This project represents a deep dive into Android's IPC (Inter-Process Communication) and WebView internals to solve a real-world productivity bottleneck.*
