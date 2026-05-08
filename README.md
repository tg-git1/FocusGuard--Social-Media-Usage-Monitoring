# MyApplication - Focus & Productivity Manager

A sophisticated Android application designed to help users maintain focus and productivity by monitoring app usage, detecting distractions, and providing intelligent productivity insights.

## Features

- **Focus Mode (Deep Work)**: Enable deep work sessions to stay focused on important tasks
- **App Usage Tracking**: Monitor and track application usage statistics
- **Distraction Detection**: Intelligent AI-powered system to detect and alert users about distractions
- **Goal Management**: Set and manage productivity goals
- **App Blocking**: Block distracting applications during focus sessions
- **Stats & Analytics**: View detailed statistics and productivity insights
- **App Selection**: Choose which apps to monitor or block
- **Notifications**: Real-time notifications to keep you informed about your focus status

## Project Structure

```
MyApplication/
├── app/                           # Main application module
│   ├── src/
│   │   └── main/
│   │       ├── AndroidManifest.xml
│   │       ├── java/              # Kotlin source files
│   │       │   ├── MainActivity.kt
│   │       │   ├── FocusService.kt
│   │       │   ├── StatsActivity.kt
│   │       │   ├── AppSelectionActivity.kt
│   │       │   ├── GoalActivity.kt
│   │       │   ├── BlockActivity.kt
│   │       │   ├── DelayActivity.kt
│   │       │   ├── Goal.kt
│   │       │   ├── SimpleLineGraph.kt
│   │       │   └── com/            # Package files
│   │       └── res/                # Resources (layouts, strings, etc.)
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/                         # Gradle wrapper configuration
├── build.gradle.kts               # Project-level build configuration
├── settings.gradle.kts
├── gradle.properties
└── local.properties               # Local environment configuration

```

## Key Components

### Activities

- **MainActivity**: Main entry point and dashboard of the application
- **StatsActivity**: Displays app usage statistics and productivity analytics
- **AppSelectionActivity**: Allows users to select which apps to monitor
- **GoalActivity**: Manage productivity goals
- **BlockActivity**: Configure app blocking rules
- **DelayActivity**: Handle activity delays or notifications

### Services

- **FocusService**: Core background service that:
  - Monitors app usage using `UsageStatsManager`
  - Detects distractions in real-time
  - Manages deep work mode timing
  - Displays overlay notifications
  - Runs AI predictor engine for intelligent recommendations
  - Maintains cumulative productivity metrics

### Models & Utilities

- **Goal**: Data model for productivity goals
- **OpenClawPredictor**: AI engine for predicting and preventing distractions
- **SimpleLineGraph**: Visualization component for displaying productivity trends

## Permissions

The app requires the following permissions:

- `FOREGROUND_SERVICE`: To run the monitoring service in foreground
- `FOREGROUND_SERVICE_SPECIAL_USE`: Special foreground service permissions
- `PACKAGE_USAGE_STATS`: To access app usage statistics
- `SYSTEM_ALERT_WINDOW`: To display overlay notifications
- `POST_NOTIFICATIONS`: To send notifications
- `QUERY_ALL_PACKAGES`: To query installed apps

## Architecture

The application follows a modular Android architecture:

1. **Foreground Service**: `FocusService` runs continuously in the background with persistent notifications
2. **AI Predictor**: Analyzes user behavior patterns to predict and prevent distractions
3. **State Management**: Tracks deep work mode, distraction metrics, and productivity data
4. **UI Layer**: Multiple activities for different features and user interactions
5. **Persistence**: Uses shared preferences/local storage for saving goals and settings

## Building & Running

### Prerequisites

- Android Studio
- Android SDK (API 24 or higher recommended)
- Gradle

### Build Steps

1. Clone or open the project in Android Studio
2. Sync Gradle files: `File > Sync Now`
3. Build the project: `Build > Make Project`
4. Run the app: `Run > Run 'app'` or press `Shift + F10`

### Configuration

1. Update `local.properties` with your SDK location if needed
2. Adjust app settings in `gradle.properties` as required

## Usage

1. **First Launch**: Grant required permissions for the app to function
2. **Setup**: Select apps to monitor in App Selection Activity
3. **Goals**: Set your productivity goals in Goal Activity
4. **Start Focus**: Enable deep work mode to start a focus session
5. **Monitor**: View real-time statistics and get AI-powered insights
6. **Manage**: Block distracting apps and adjust settings as needed

## Technical Details

- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Minimum SDK**: API 24
- **Architecture**: Service-based with Activities for UI
- **Data Format**: JSON (using Gson for serialization)

## Development

### Key Files to Modify

- `FocusService.kt`: Core service logic and background operations
- `MainActivity.kt`: Main UI and dashboard
- Update activities for new features

### Adding New Features

1. Create new Activity files in `java/` directory
2. Register in `AndroidManifest.xml`
3. Update navigation logic in existing activities
4. Add UI resources in `res/` directory

## Troubleshooting

- **Service not running**: Check that foreground service permissions are granted
- **Stats not updating**: Ensure app usage access permission is enabled in system settings
- **Notifications not showing**: Verify POST_NOTIFICATIONS permission is granted
- **Build errors**: Run `Build > Clean Project` and rebuild

## Future Enhancements

- Cloud synchronization for goals and statistics
- Customizable distraction detection algorithms
- Enhanced AI model for better predictions
- Social features for accountability
- Integration with calendar and task management


**Note**: This app requires system-level permissions to monitor app usage. Ensure you have granted all necessary permissions in system settings for optimal functionality.
