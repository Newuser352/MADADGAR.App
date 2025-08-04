# MADADGAR App Data Models Documentation

## Core Entities

### User
Represents an application user, owning items and receiving notifications.

### Item
- **Represents:** Tangible entities in the app.
- **Attributes:** id, title, description, categories, owner, location, timestamps, etc.

### Notification
Handled by `UserNotification` and `NewUserNotification` models.
- **Attributes:** id, type, title, body, payload, isRead, userRef, timestamps.

### Device Token
Handled by `UserDeviceToken` and `DeviceTokenUpdate` models.
- **Attributes:** id, userRef, deviceToken, platform, isActive, timestamps.

### Category
Classifies items under various types.

## Relationships
- **User - Item:** Users create and own items.
- **User - Notification:** Users receive notifications specific to actions/events.
- **User - Device Token:** Each user has device tokens for push notifications.
- **Item - Category:** Items can have one or more categories.

## ER Diagram
![ER Diagram](er_diagram.png)

---

## Table Columns and Constraints
- **UserNotification**: Primary key `id`, foreign key `userId`.
- **UserDeviceToken**: Primary key `id`, foreign key `userId`.

## Diagram Legend and Notations
- **Rectangles:** Entities/Tables.
- **Diamonds:** Relationships.
- **Ovals:** Attributes.

## Explanation
The ER diagram visually presents the interrelations among the core entities of the MADADGAR app, providing a clear overview of user interactions with items and notifications, along with device token management.
