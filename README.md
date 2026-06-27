# Liquor Ledger

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)
![Firestore](https://img.shields.io/badge/Firestore-039BE5?style=for-the-badge&logo=firebase&logoColor=white)
![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84?style=for-the-badge&logo=androidstudio&logoColor=white)

A fully functional Android tablet POS (Point of Sale) system built for liquor store management. Liquor Ledger handles inventory tracking, purchase orders, employee timecards, user management, and sales transactions — all backed by Firebase Firestore with real-time data sync.

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Firebase Setup](#firebase-setup)
- [Security](#security)
- [Installation](#installation)
- [Team](#team)

---

## Overview

Liquor Ledger was built as a semester-long class project at Full Sail University. The goal was to design and develop a production-quality Android tablet POS application that a real liquor store could use to manage day-to-day operations. The app supports two roles — **Manager** and **Cashier** — with role-based access control enforced throughout.

---

## Features

### Authentication
- Employee ID login using last 4 digits for fast access
- Firebase Authentication with Email/Password under the hood
- Account lockout after 5 failed login attempts
- Manager can unlock locked accounts via the User Info page
- Animated splash screen with Firebase warmup on launch
- Session always cleared on app launch for security

### POS / Register
- Full point of sale interface with product catalog
- Cart management with line item tracking
- Cash payment flow with change due calculation
- Transaction recording to Firestore

### Inventory
- Full product table with all columns: Product, SKU, Category, Vendor, Stock, Reorder Point, Cost, Tax%, Margin%, Stock Value, Status
- Low stock indicators (amber) and out of stock indicators (red)
- Summary stats bar: Total Products, Inventory Value, Low Stock count, Out of Stock count
- Search by product name, SKU, or vendor
- Filter by category
- Add new products via dialog
- Edit existing products via dialog
- Adjust stock via product dropdown — no manual name entry required

### Purchase Orders
- Full PO workflow: Pending Review → Submitted → Received
- Create new orders with product line items pulled from Firestore
- Product dropdown on new order form ensures names match inventory exactly
- Auto-incrementing PO numbers (PO-0001, PO-0002, etc.)
- Filter POs by status with persistent highlight
- Delete POs in pending review status
- Receiving checklist on delivery — confirm quantity received per item
- Partial receipt support — enter actual quantities received
- Inventory stock automatically updated on receipt based on what was actually received

### Timecard
- Weekly timesheet view with full week navigation
- Clock In / Clock Out with confirmation dialog
- Break Out / Break In toggle — tracks break time accurately
- Hours worked calculated automatically with break time deducted
- Day Off status for days with no clock in
- Manager employee dropdown to view any employee's timesheet
- Manager can edit any timecard entry with time format validation
- Clock out cannot be set before clock in

### Timecard Reports
- Manager-only report accessible via button inside the Timecard page
- Shows all employees and their weekly attendance records
- Filterable by week with navigation
- Total hours per employee per week
- Back button returns to Timecard page

### User Info
- Profile card showing Employee ID, Name, and Position for logged in employee
- Manager employee list with search by name or Employee ID
- Edit employee name, position, and Employee ID
- Position change requires confirmation dialog
- Set password dialog — starred input, confirm password, minimum 6 characters
- Add new employee — auto-generates Employee ID in EMP-YYYY-NNNN format
- Unlock locked accounts with confirmation dialog

### Reports
- Sales Analytics with custom bar chart built on Canvas
- Summary cards for Total Income, Total Expenses, and Net
- Sales Report, Inventory Report, and Inventory Alert sub-pages

### Settings
- Dark Mode toggle
- Colorblind Mode toggle
- Preferences persist across sessions via SharedPreferences

### Emergency Contacts
- Store emergency contact information

### Navigation
- Scrollable sidebar navigation
- Role-based tab visibility — Purchase Orders and Timecard Reports hidden from Cashiers
- Active tab highlight
- Logout button always visible at the bottom of the sidebar

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Android Views (Activity-based, no Compose) |
| Database | Firebase Firestore |
| Authentication | Firebase Authentication |
| Architecture | Activity + Page class pattern |
| Async | Kotlin Coroutines |
| IDE | Android Studio |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| Build System | Gradle with Version Catalog |

---

## Architecture

The app uses a single `MainActivity` that manages sidebar navigation and swaps page content into a `contentBox` container. Each screen is a self-contained Kotlin class with a `build()` function that returns a `LinearLayout`.

```
app/
└── src/main/java/com/liquor/ledger/
    ├── SplashActivity.kt          — Animated splash, Firebase warmup
    ├── LoginActivity.kt           — Employee ID login with lockout
    ├── MainActivity.kt            — Sidebar navigation, page routing
    ├── Employee.kt                — Data class for employee model
    ├── SessionManager.kt          — Singleton for current session
    ├── InventoryPage.kt           — Inventory management
    ├── PurchaseOrdersPage.kt      — Purchase order workflow
    ├── TimecardPage.kt            — Employee timesheet and clocking
    ├── TimecardReportsPage.kt     — Manager attendance report
    ├── UserInfoPage.kt            — Employee profile and management
    ├── POSPage.kt                 — Point of sale register
    ├── ReportsPage.kt             — Sales analytics
    ├── SalesReportPage.kt         — Sales report sub-page
    ├── InventoryReportPage.kt     — Inventory report sub-page
    ├── InventoryAlertPage.kt      — Inventory alert sub-page
    ├── SettingsPage.kt            — App settings
    ├── EmergencyContactsPage.kt   — Emergency contacts
    └── firebase/
        ├── FirebaseManager.kt     — Singleton Firebase access point
        └── AuthRepository.kt      — Authentication and lockout logic
```

---

## Firebase Setup

The app uses the following Firestore collections:

### employees
| Field | Type | Description |
|---|---|---|
| employeeId | string | e.g. EMP-2024-0001 |
| name | string | Full name |
| position | string | Manager or Cashier |
| email | string | Firebase Auth email |
| uid | string | Firebase Auth UID |
| failedAttempts | int64 | Failed login counter |
| isLocked | boolean | Account lock status |

### products
| Field | Type | Description |
|---|---|---|
| name | string | Product name |
| sku | string | Stock keeping unit |
| category | string | Product category |
| vendor | string | Vendor name |
| stock | int64 | Current stock level |
| reorderPoint | int64 | Low stock threshold |
| cost | double | Cost per unit |
| price | double | Selling price |
| taxPercent | double | Tax percentage |
| marginPercent | double | Margin percentage |

### purchaseOrders
| Field | Type | Description |
|---|---|---|
| poNumber | string | Auto-incremented e.g. PO-0001 |
| vendor | string | Vendor name |
| date | timestamp | Order date |
| total | double | Order total |
| status | string | pending review / submitted / received |
| notes | string | Optional notes |
| items | array | Line items with productName, quantity, costPerUnit |
| receivedItems | array | Actual received quantities on delivery |

### timecards
| Field | Type | Description |
|---|---|---|
| employeeId | string | Links to employee |
| employeeName | string | For display |
| date | string | yyyy-MM-dd format |
| dayOfWeek | string | e.g. Monday |
| clockIn | timestamp | Clock in time |
| clockOut | timestamp | Clock out time |
| breakStart | timestamp | Break start time |
| breakEnd | timestamp | Break end time |
| breakMinutes | int64 | Total break minutes |
| hoursWorked | double | Net hours after break |
| status | string | In Progress / Completed |

### transactions
| Field | Type | Description |
|---|---|---|
| items | array | Cart line items |
| total | double | Transaction total |
| amountPaid | double | Amount paid |
| changeDue | double | Change returned |
| timestamp | timestamp | Transaction time |
| employeeId | string | Who processed it |

---

## Security

Firestore security rules require authentication for all reads and writes except the employees collection read — which must be public to allow the Employee ID lookup during login before authentication is established.

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    match /employees/{document} {
      allow read: if true;
      allow write: if request.auth != null;
    }

    match /products/{document} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    match /transactions/{document} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    match /purchaseOrders/{document} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }

    match /timecards/{document} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}
```

Role-based access control (Manager vs Cashier) is enforced at the application level via `SessionManager`. The Manager role unlocks Purchase Orders, Timecard Reports, employee management, and the ability to edit timecards and inventory.

---

## Installation

1. Clone the repository
2. Open the project in Android Studio
3. Obtain `google-services.json` from the project Firebase console and place it in the `app/` directory
4. Sync Gradle
5. Build and run on an Android tablet (Samsung SM-T738U recommended, min SDK 26)

> **Note:** `google-services.json` is excluded from version control. Contact a team member to obtain the file.

---

## Team

| Name | Role | Contributions |
|---|---|---|
| Jonathan Smith | Firebase Developer & Backend | Firebase setup, Authentication, Login, Splash Screen, Inventory, Purchase Orders, Timecard, Timecard Reports, User Info |
| Antonio Vega | UI Developer | MainActivity, Sidebar Navigation, Reports, Emergency Contacts, UI Design |
| Robert Kearney | POS Developer | POS / Register page, Cart management, Payment flow |
| Alejandro Figueroa | Settings Developer | Settings page, Dark Mode, Colorblind Mode |

---

*Built at Full Sail University — 2026*
