# 🔕 Smart Notification Suppression

## Overview
Prevents sending push notifications to users who are actively viewing the chat, reducing notification spam and improving UX.

## How It Works

### 1. Presence Tracking
- `user_presence` table tracks `current_chat_id` for each user
- Updated when user enters/exits a chat screen
- Cleared when user leaves the chat

### 2. Notification Check
Before sending a chat notification, the system checks:
- Is the recipient online? (`is_online = true`)
- Are they in this specific chat? (`current_chat_id = chatId`)
- Is their presence recent? (`last_seen` within 5 minutes)

If all conditions are true → **Skip notification** ✅

### 3. Architecture

```
ChatScreen → ChatPresenceTracker → UpdateCurrentChatUseCase → PresenceRepository
                                                                      ↓
                                                              user_presence.current_chat_id

SendMessage → ChatRepository → Check isUserInChat() → Send notification (if not in chat)
```

## Implementation

### Backend (Already exists)
The `user_presence` table already has `current_chat_id` column.

### Domain Layer (Shared KMP)

**Interface**: `PresenceRepository`
```kotlin
suspend fun updatePresence(isOnline: Boolean, currentChatId: String? = null): Result<Unit>
suspend fun isUserInChat(userId: String, chatId: String): Boolean
```

**Use Case**: `UpdateCurrentChatUseCase`
```kotlin
class UpdateCurrentChatUseCase(
    private val presenceRepository: PresenceRepository
) {
    suspend operator fun invoke(chatId: String?) = 
        presenceRepository.updatePresence(isOnline = true, currentChatId = chatId)
}
```

### Data Layer (Shared KMP)

**Repository**: `SupabasePresenceRepository`
- `updatePresence()` - Updates `current_chat_id` in database
- `isUserInChat()` - Checks if user is actively in a specific chat

**Chat Repository**: `SupabaseChatRepository`
```kotlin
// Before sending notification
val isInChat = presenceRepository?.isUserInChat(recipientId, chatId) ?: false
if (!isInChat) {
    dataSource.sendMessageNotification(...)
}
```

### Presentation Layer (Android)

**Component**: `ChatPresenceTracker`
```kotlin
@Composable
fun ChatPresenceTracker(
    chatId: String,
    updateCurrentChat: suspend (String?) -> Result<Unit>
)
```

## Usage

### In ChatScreen/ChatDetailScreen

```kotlin
@Composable
fun ChatScreen(
    chatId: String,
    updateCurrentChat: UpdateCurrentChatUseCase = hiltViewModel()
) {
    // Track presence - sets current_chat_id on enter, clears on exit
    ChatPresenceTracker(
        chatId = chatId,
        updateCurrentChat = updateCurrentChat
    )
    
    // Rest of your chat UI
    // ...
}
```

### In ChatViewModel (if needed)

```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val updateCurrentChatUseCase: UpdateCurrentChatUseCase
) : ViewModel() {
    
    fun onChatOpened(chatId: String) {
        viewModelScope.launch {
            updateCurrentChatUseCase(chatId)
        }
    }
    
    fun onChatClosed() {
        viewModelScope.launch {
            updateCurrentChatUseCase(null)
        }
    }
}
```

## Benefits

✅ **No spam**: Users don't get notified for messages they're already reading  
✅ **Better UX**: Notifications only when actually needed  
✅ **Minimal overhead**: Single database field update  
✅ **Clean Architecture**: Follows domain-driven design  
✅ **Automatic**: Works with existing presence tracking  

## Edge Cases Handled

1. **User closes app while in chat**: Presence tracking stops, `is_online` becomes false
2. **User switches chats**: `current_chat_id` updates to new chat
3. **User goes to home screen**: `current_chat_id` cleared via `onDispose`
4. **Network issues**: Defaults to sending notification (fail-safe)
5. **Presence repository unavailable**: Sends notification (backward compatible)

## Testing

### Manual Test
1. Open chat with User A on Device 1
2. Send message from User B on Device 2
3. **Expected**: Device 1 should NOT receive notification
4. Exit chat on Device 1
5. Send another message from Device 2
6. **Expected**: Device 1 SHOULD receive notification

### Unit Test
```kotlin
@Test
fun `should not send notification when user is in chat`() = runTest {
    // Given
    val recipientId = "user123"
    val chatId = "chat456"
    coEvery { presenceRepository.isUserInChat(recipientId, chatId) } returns true
    
    // When
    chatRepository.sendMessage(chatId, "Hello")
    
    // Then
    coVerify(exactly = 0) { dataSource.sendMessageNotification(any(), any(), any(), any()) }
}
```

## Files Modified

### Shared Module (KMP)
- ✅ `PresenceRepository.kt` - Added `currentChatId` parameter and `isUserInChat()` method
- ✅ `SupabasePresenceRepository.kt` - Implemented chat tracking logic
- ✅ `SupabaseChatRepository.kt` - Added presence check before notification
- ✅ `UpdateCurrentChatUseCase.kt` - New use case

### Android App
- ✅ `PresenceModule.kt` - Added DI for new use case
- ✅ `RepositoryModule.kt` - Injected PresenceRepository into ChatRepository
- ✅ `ChatPresenceTracker.kt` - New composable utility

## Next Steps

1. **Integrate**: Add `ChatPresenceTracker` to your chat screen(s)
2. **Test**: Verify notifications are suppressed when in chat
3. **Monitor**: Check logs for "NOTIFICATION" tag to see when notifications are skipped

## Configuration

All behavior is automatic. No configuration needed.

## Troubleshooting

**Notifications still sent when in chat?**
- Check if `ChatPresenceTracker` is added to chat screen
- Verify `current_chat_id` is being set in database
- Check logs for "NOTIFICATION" tag

**Notifications not sent when should be?**
- Verify presence tracking is working
- Check `is_online` status in database
- Ensure `last_seen` is recent (< 5 minutes)

---

**Status**: ✅ Implementation complete  
**Architecture**: ✅ Clean Architecture compliant  
**Testing**: Manual testing required  
**Documentation**: ✅ Complete
