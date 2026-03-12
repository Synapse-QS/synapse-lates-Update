# Active Status Feature

## Overview
The active status feature shows a green indicator next to users who are currently online and active in the app.

## How It Works

### Backend
- Uses the existing `user_presence` table in Supabase
- A user is considered "active" if:
  - `is_online` is `true`
  - `last_seen` timestamp is within the last 5 minutes

### Database
- **Function**: `is_user_active(user_last_seen)` - Helper function to check if a timestamp is within the active window
- **Indexes**: Added for performance on `last_seen` and `is_online` columns

### Architecture

#### Domain Layer
- **Model**: `User.isActive` - Computed property based on status and last_seen
- **Repository**: `PresenceRepository` - Interface for presence operations
- **Use Cases**:
  - `UpdatePresenceUseCase` - Update user's online status
  - `StartPresenceTrackingUseCase` - Start heartbeat tracking
  - `ObserveUserActiveStatusUseCase` - Observe another user's active status

#### Data Layer
- **Repository Implementation**: `SupabasePresenceRepository`
  - Polls user_presence table every 10 seconds
  - Sends heartbeat every 30 seconds when user is active
  - Validates 5-minute active window

#### Presentation Layer
- **ViewModel**: `UserPresenceViewModel` - Manages presence state
- **Components**:
  - `ActiveStatusIndicator` - Green dot indicator
  - `UserAvatar` - Basic avatar component
  - `UserAvatarWithStatus` - Avatar with active status overlay
  - `UserListItem` - Example usage in a list

## Usage

### Show Active Status on Avatar
```kotlin
UserAvatarWithStatus(
    userId = user.uid,
    avatarUrl = user.avatar,
    size = 48.dp,
    showActiveStatus = true
)
```

### Observe User Status
```kotlin
val viewModel: UserPresenceViewModel = hiltViewModel()
val isActive by viewModel.observeUserStatus(userId).collectAsState(initial = false)
```

### Start Presence Tracking (in MainActivity or App)
```kotlin
val startPresenceTracking: StartPresenceTrackingUseCase = /* inject */
LaunchedEffect(Unit) {
    startPresenceTracking()
}
```

## Customization

### Change Active Window
Modify the `isWithinActiveWindow` function in `SupabasePresenceRepository`:
```kotlin
diff.inWholeMinutes < 5  // Change 5 to desired minutes
```

### Change Indicator Color
Modify `ActiveStatusIndicator.kt`:
```kotlin
.background(Color(0xFF44B700))  // Change to desired color
```

### Change Polling Interval
Modify `observeUserPresence` in `SupabasePresenceRepository`:
```kotlin
delay(10_000)  // Change to desired milliseconds
```

## Performance Considerations
- Polling every 10 seconds per observed user
- Consider implementing Supabase Realtime subscriptions for better performance with many users
- Heartbeat runs every 30 seconds when app is active

## Future Improvements
- [ ] Implement Supabase Realtime Presence API instead of polling
- [ ] Add "Last seen X minutes ago" text
- [ ] Add privacy settings to hide active status
- [ ] Batch presence queries for multiple users
- [ ] Add offline status with last seen time
