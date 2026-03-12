# Active Status Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                         Presentation Layer                       │
│                          (Android App)                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │ UserAvatarWith   │         │ UserPresence     │             │
│  │ Status           │────────▶│ ViewModel        │             │
│  │ @Composable      │         │                  │             │
│  └──────────────────┘         └──────────────────┘             │
│           │                             │                        │
│           │                             │                        │
│           ▼                             ▼                        │
│  ┌──────────────────┐         ┌──────────────────┐             │
│  │ ActiveStatus     │         │ PresenceTracker  │             │
│  │ Indicator        │         │ @Composable      │             │
│  └──────────────────┘         └──────────────────┘             │
│                                         │                        │
└─────────────────────────────────────────┼────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                          Domain Layer                            │
│                        (Shared KMP Logic)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    Use Cases                              │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  • ObserveUserActiveStatusUseCase                        │  │
│  │  • UpdatePresenceUseCase                                 │  │
│  │  • StartPresenceTrackingUseCase                          │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                   │
│                              ▼                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              PresenceRepository (Interface)               │  │
│  │  • updatePresence(isOnline: Boolean)                     │  │
│  │  • startPresenceTracking()                               │  │
│  │  • observeUserPresence(userId): Flow<Boolean>            │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                    User Model                             │  │
│  │  • isActive: Boolean (computed property)                 │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                           Data Layer                             │
│                        (Shared KMP Logic)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │         SupabasePresenceRepository (Implementation)       │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  • Heartbeat Job (30s interval)                          │  │
│  │  • Polling (10s interval per user)                       │  │
│  │  • 5-minute active window validation                     │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                   │
└──────────────────────────────┼───────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Backend (Supabase)                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  user_presence Table                      │  │
│  ├──────────────────────────────────────────────────────────┤  │
│  │  • user_id (text, unique)                                │  │
│  │  • is_online (boolean)                                   │  │
│  │  • last_seen (timestamptz)                               │  │
│  │  • activity_status (text)                                │  │
│  │  • current_chat_id (text)                                │  │
│  └──────────────────────────────────────────────────────────┘  │
│                              │                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │              is_user_active() Function                    │  │
│  │  Returns: boolean (last_seen within 5 minutes)           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                      Indexes                              │  │
│  │  • idx_user_presence_last_seen                           │  │
│  │  • idx_user_presence_is_online                           │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Presence Update (Heartbeat)
```
User Opens App
     │
     ▼
PresenceTracker
     │
     ▼
StartPresenceTrackingUseCase
     │
     ▼
SupabasePresenceRepository
     │
     ├─▶ Start Heartbeat Job (every 30s)
     │        │
     │        ▼
     │   updatePresence(true)
     │        │
     │        ▼
     └─▶ Supabase: UPDATE user_presence
              SET is_online = true,
                  last_seen = NOW()
```

### 2. Active Status Observation
```
UserAvatarWithStatus(userId)
     │
     ▼
UserPresenceViewModel
     │
     ▼
ObserveUserActiveStatusUseCase
     │
     ▼
SupabasePresenceRepository.observeUserPresence()
     │
     ├─▶ Poll Database (every 10s)
     │        │
     │        ▼
     │   SELECT * FROM user_presence
     │   WHERE user_id = ?
     │        │
     │        ▼
     │   Check: is_online AND (NOW() - last_seen < 5 min)
     │        │
     │        ▼
     └─▶ Emit: Flow<Boolean>
              │
              ▼
         UI Updates (Green Dot)
```

### 3. Cleanup (User Closes App)
```
User Closes App
     │
     ▼
PresenceTracker.onDispose()
     │
     ▼
UpdatePresenceUseCase(false)
     │
     ▼
SupabasePresenceRepository
     │
     ├─▶ Cancel Heartbeat Job
     │
     └─▶ Supabase: UPDATE user_presence
              SET is_online = false,
                  last_seen = NOW()
```

## Component Relationships

```
┌─────────────────────────────────────────────────────────┐
│                    UI Components                         │
├─────────────────────────────────────────────────────────┤
│                                                           │
│  UserAvatarWithStatus                                    │
│         │                                                 │
│         ├─▶ UserAvatar (displays image)                 │
│         │                                                 │
│         └─▶ ActiveStatusIndicator (green dot)           │
│                    │                                      │
│                    └─▶ observeUserStatus(userId)        │
│                              │                            │
│                              ▼                            │
│                    UserPresenceViewModel                 │
│                              │                            │
└──────────────────────────────┼──────────────────────────┘
                               │
                               ▼
                    ObserveUserActiveStatusUseCase
                               │
                               ▼
                    PresenceRepository.observeUserPresence()
```

## Timing Diagram

```
Time    User A                  Database                User B
─────────────────────────────────────────────────────────────────
0:00    Opens App              
        │                       
0:01    ├─▶ is_online=true     
        │   last_seen=0:01      
        │                       
0:30    ├─▶ Heartbeat          
        │   last_seen=0:30      
        │                                               Opens App
        │                                                   │
1:00    ├─▶ Heartbeat                                      │
        │   last_seen=1:00                                 │
        │                                               ├─▶ Polls
        │                       User A: Active ◀────────┤   
        │                       (last_seen=1:00)           │
1:30    ├─▶ Heartbeat                                      │
        │   last_seen=1:30                                 │
        │                                                   │
2:00    Closes App                                         │
        │                                                   │
2:01    ├─▶ is_online=false                                │
        │   last_seen=2:01                                 │
        │                                               ├─▶ Polls
        │                       User A: Inactive ◀──────┤
        │                       (last_seen=2:01)           │
6:00                                                        │
                                                        ├─▶ Polls
                                User A: Inactive ◀──────┤
                                (last_seen > 5 min)        │
```

## Key Design Decisions

1. **5-Minute Active Window**: Balances real-time accuracy with user privacy
2. **30-Second Heartbeat**: Frequent enough for accuracy, light on resources
3. **10-Second Polling**: Smooth UI updates without excessive queries
4. **Computed Property**: `User.isActive` keeps logic in domain layer
5. **Flow-Based**: Reactive updates for real-time UI changes

## Performance Characteristics

- **Database Writes**: 1 per 30 seconds per active user
- **Database Reads**: 1 per 10 seconds per observed user
- **Network Overhead**: ~100 bytes per heartbeat
- **Memory**: Minimal (one Flow per observed user)
- **CPU**: Negligible (background coroutines)

## Future Optimizations

1. **Supabase Realtime**: Replace polling with WebSocket subscriptions
2. **Batch Queries**: Query multiple users in single request
3. **Local Caching**: Cache status for 10 seconds to reduce queries
4. **Smart Polling**: Only poll for visible users
