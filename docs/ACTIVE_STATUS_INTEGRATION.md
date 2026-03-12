# Active Status Integration Guide

## Quick Integration Checklist

### Step 1: Initialize Presence Tracking
Add to your main activity or root composable:

```kotlin
// In MainActivity.kt or App.kt
@Composable
fun SynapseApp() {
    val startPresenceTracking: StartPresenceTrackingUseCase = hiltViewModel<MainViewModel>().startPresenceTracking
    val updatePresence: UpdatePresenceUseCase = hiltViewModel<MainViewModel>().updatePresence
    
    PresenceTracker(
        startPresenceTracking = startPresenceTracking,
        updatePresence = updatePresence
    )
    
    // Your existing app content
    NavHost(...)
}
```

### Step 2: Replace Existing Avatars

#### Before:
```kotlin
AsyncImage(
    model = user.avatar,
    contentDescription = "Avatar",
    modifier = Modifier.size(48.dp).clip(CircleShape)
)
```

#### After:
```kotlin
UserAvatarWithStatus(
    userId = user.uid,
    avatarUrl = user.avatar,
    size = 48.dp,
    showActiveStatus = true
)
```

### Step 3: Update Chat List Items

```kotlin
@Composable
fun ChatListItem(conversation: Conversation) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Replace old avatar with:
        UserAvatarWithStatus(
            userId = conversation.otherUserId,
            avatarUrl = conversation.otherUserAvatar,
            size = 56.dp,
            showActiveStatus = true
        )
        
        // Rest of your chat item UI
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(conversation.name)
            Text(conversation.lastMessage)
        }
    }
}
```

### Step 4: Update Profile Screens

```kotlin
@Composable
fun ProfileHeader(user: User) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        UserAvatarWithStatus(
            userId = user.uid,
            avatarUrl = user.avatar,
            size = 120.dp,
            showActiveStatus = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(user.displayName ?: user.username ?: "")
    }
}
```

### Step 5: Update User Search Results

```kotlin
@Composable
fun SearchResultItem(user: User, onClick: () -> Unit) {
    UserListItem(user = user)  // Uses active status by default
}
```

## Common Patterns

### Pattern 1: Conditional Active Status
Show active status only for non-blocked users:
```kotlin
UserAvatarWithStatus(
    userId = user.uid,
    avatarUrl = user.avatar,
    size = 48.dp,
    showActiveStatus = !user.banned && !isBlocked
)
```

### Pattern 2: Different Sizes
```kotlin
// Small (chat list)
UserAvatarWithStatus(userId = id, avatarUrl = url, size = 40.dp)

// Medium (profile preview)
UserAvatarWithStatus(userId = id, avatarUrl = url, size = 56.dp)

// Large (profile header)
UserAvatarWithStatus(userId = id, avatarUrl = url, size = 120.dp)
```

### Pattern 3: Custom Indicator Size
The indicator automatically scales to 30% of avatar size, but you can customize:
```kotlin
Box {
    UserAvatar(avatarUrl = url, size = 48.dp)
    ActiveStatusIndicator(
        isActive = isActive,
        size = 16.dp,  // Custom size
        modifier = Modifier.align(Alignment.BottomEnd)
    )
}
```

## Testing

### Test Active Status Display
1. Open app on two devices with different accounts
2. Verify green dot appears on active user
3. Close app on one device
4. Wait 5 minutes
5. Verify green dot disappears

### Test Heartbeat
1. Open app
2. Check database: `SELECT * FROM user_presence WHERE user_id = 'YOUR_ID'`
3. Verify `last_seen` updates every 30 seconds
4. Verify `is_online` is `true`

### Test Cleanup
1. Close app
2. Check database
3. Verify `is_online` is `false`

## Troubleshooting

### Active Status Not Showing
- Check if `PresenceTracker` is added to app root
- Verify user is authenticated
- Check database for `user_presence` entry
- Verify `last_seen` is recent

### Performance Issues
- Reduce polling interval in `SupabasePresenceRepository`
- Implement Supabase Realtime instead of polling
- Batch presence queries for multiple users

### Indicator Not Visible
- Check if `showActiveStatus = true`
- Verify indicator color contrasts with background
- Check if avatar size is large enough (min 24.dp recommended)

## Migration Checklist

- [ ] Add `PresenceTracker` to app root
- [ ] Replace avatars in chat list
- [ ] Replace avatars in user search
- [ ] Replace avatars in profile screens
- [ ] Replace avatars in comments/posts
- [ ] Replace avatars in notifications
- [ ] Test on multiple devices
- [ ] Verify database updates
- [ ] Check performance with many users
- [ ] Update any custom avatar implementations

## Performance Tips

1. **Lazy Loading**: Only observe presence for visible users
2. **Caching**: Consider caching presence state for 10 seconds
3. **Batching**: Group presence queries when possible
4. **Realtime**: Migrate to Supabase Realtime for production

## Next Steps

After basic integration:
1. Add privacy settings (allow users to hide active status)
2. Add "Last seen" text for offline users
3. Implement Supabase Realtime for better performance
4. Add analytics to track feature usage
