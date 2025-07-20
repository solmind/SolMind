# AI Ledger for Solana Mobile

An AI-powered expense tracking Android app built for the Solana Mobile Hackathon. This app combines artificial intelligence with blockchain technology to provide intelligent expense categorization and automatic Solana transaction tracking.

## Features

### 🤖 AI-Powered Expense Classification
- **Smart Text Recognition**: Extract transaction details from receipt photos using ML Kit
- **Intelligent Categorization**: Automatically categorize expenses using AI algorithms
- **Confidence Scoring**: Display AI confidence levels for categorization accuracy
- **Manual Override**: Easy manual editing and correction of AI suggestions

### 🔗 Solana Blockchain Integration
- **Wallet Management**: Add and manage multiple Solana wallet addresses
- **Automatic Sync**: Real-time synchronization of on-chain transactions
- **Transaction Parsing**: Intelligent parsing of Solana transaction data
- **Balance Tracking**: Monitor SOL balances across connected wallets

### 📱 Modern Android UI
- **Material 3 Design**: Beautiful, modern interface following Material Design guidelines
- **Jetpack Compose**: Built with the latest Android UI toolkit
- **Dark/Light Theme**: Adaptive theming support
- **Responsive Layout**: Optimized for various screen sizes

### 📊 Comprehensive Tracking
- **Multiple Categories**: Food, Transportation, Shopping, Entertainment, Bills, Healthcare, Education, Investment, and more
- **Income & Expense Tracking**: Complete financial overview
- **Monthly Summaries**: Detailed monthly income and expense breakdowns
- **Search & Filter**: Advanced filtering by category, date, and amount

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository Pattern
- **Dependency Injection**: Hilt
- **Database**: Room (SQLite)
- **AI/ML**: ML Kit for text recognition
- **Blockchain**: Solana Web3.js integration
- **Camera**: CameraX
- **Navigation**: Jetpack Navigation Compose

## Project Structure

```
app/src/main/java/com/solana/ailedger/
├── data/
│   ├── database/          # Room database, DAOs, and entities
│   ├── model/            # Data models and entities
│   ├── repository/       # Repository pattern implementation
│   └── service/          # AI and Solana services
├── di/                   # Dependency injection modules
├── ui/
│   ├── screens/          # Compose UI screens
│   ├── theme/            # Material 3 theming
│   └── viewmodel/        # ViewModels for UI state management
└── AILedgerApplication.kt # Application class
```

## Key Components

### AI Service (`AIService.kt`)
- Text extraction from images using ML Kit
- Transaction categorization based on keywords
- Amount and date extraction from text
- Confidence scoring for AI predictions

### Solana Service (`SolanaService.kt`)
- Solana RPC integration
- Transaction history retrieval
- Balance checking
- Address validation

### Database Layer
- **LedgerEntry**: Core transaction data model
- **SolanaWallet**: Wallet management
- **Room Database**: Local data persistence

### UI Screens
- **HomeScreen**: Dashboard with balance overview and recent transactions
- **AddEntryScreen**: Manual transaction entry with AI assistance and integrated camera functionality
- **WalletScreen**: Solana wallet management
- **SettingsScreen**: App configuration and preferences

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24 (API level 24) or higher
- Kotlin 1.8.0 or later

### Installation

1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd solana-hackerthon
   ```

2. Open the project in Android Studio

3. Sync the project with Gradle files

4. Build and run the app:
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

### Configuration

1. **Android SDK**: Ensure Android SDK is properly configured in `local.properties`
2. **Permissions**: The app requires camera and internet permissions
3. **Solana Network**: Currently configured for Solana mainnet

## Usage

### Adding Transactions

1. **Manual Entry**: Use the "Add Entry" screen to manually input transaction details
2. **Camera Capture**: Use the integrated camera feature in the "Add Entry" screen to take photos of receipts and let AI extract the details
3. **AI Assistance**: Get intelligent suggestions for categorization

### Managing Wallets

1. **Add Wallet**: Enter a Solana wallet address and give it a name
2. **Sync Transactions**: Automatically fetch and categorize on-chain transactions
3. **Monitor Balance**: Track SOL balances across all connected wallets

### Viewing Reports

1. **Dashboard**: View monthly income/expense summaries
2. **Transaction History**: Browse all transactions with filtering options
3. **Category Breakdown**: Analyze spending patterns by category

## Contributing

This project was built for the Solana Mobile Hackathon. Contributions and improvements are welcome!

### Development Guidelines

1. Follow Kotlin coding conventions
2. Use Jetpack Compose for UI development
3. Implement proper error handling
4. Add unit tests for business logic
5. Follow Material Design guidelines

## License

This project is open source and available under the [MIT License](LICENSE).

## Acknowledgments

- Built for the Solana Mobile Hackathon
- Powered by Solana blockchain technology
- Uses Google ML Kit for AI capabilities
- Designed with Material 3 guidelines

## Contact

For questions or support, please open an issue in the repository.

---

**Built with ❤️ for the Solana Mobile Hackathon**