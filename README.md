# Flippa Clone - Production-Grade Marketplace Platform

A fully production-grade Spring Boot application for buying and selling online businesses, social media accounts, and domains. This platform includes a complete escrow system, payment gateway integrations, automatic website analytics fetching, and comprehensive admin controls.

## 🚀 Features

### Core Features
- **User Authentication & Registration**: Self-registration with email verification support
- **Buy & Sell Listings**: Users can create listings and purchase from others
- **Escrow System**: Secure escrow service for safe transfer of ownership
- **Payment Gateways**: Integrated support for PayPal and PayNow Zimbabwe
- **Automatic Website Info Fetching**: Automatically fetches traffic, revenue, and analytics data from listed websites
- **Admin Dashboard**: Comprehensive admin panel for managing users, listings, disputes, and system settings
- **Audit Logging**: Complete audit trail of all system actions
- **Role-Based Access Control**: Multiple user roles (User, Seller, Buyer, Admin, Super Admin)

### Admin Features
- **User Management**: Ban/unban users, toggle user roles
- **Listing Management**: Approve/activate listings
- **Dispute Resolution**: Resolve escrow disputes
- **System Configuration**: Toggle payment gateways, configure system settings
- **Payment Gateway Control**: Enable/disable payment gateways dynamically

### Technical Features
- **Flyway Migrations**: Database versioning and continuous development support
- **Comprehensive Logging**: Structured logging for all processes
- **Production-Ready**: Pure OOP architecture with entities, DTOs, services, and controllers
- **Security**: Spring Security with role-based access control
- **H2 Database**: Embedded database for v1 (easily upgradeable to PostgreSQL/MySQL)

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.2.0
- **View**: Thymeleaf
- **Database**: H2 (v1)
- **Security**: Spring Security
- **Migrations**: Flyway
- **Build Tool**: Maven
- **Java Version**: 17

## 📋 Prerequisites

- Java 17 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## 🔧 Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/DexterWura/Flippa-clone.git
cd Flippa-clone
```

### 2. Configure Application Properties

Edit `src/main/resources/application.yml` if needed. Note that payment gateway settings are now managed through the admin UI in the database.

```yaml
# JWT Secret (change in production!)
app:
  jwt:
    secret: your-256-bit-secret-key-change-this-in-production-minimum-32-characters
```

**Payment Gateway Configuration**: After starting the application, login as admin and navigate to `/admin/settings` to configure:
- PayPal Client ID and Secret
- PayPal Mode (sandbox/live)
- PayNow Zim Integration ID and Key
- PayNow Zim Return and Result URLs
- Enable/disable payment gateways

### 3. Build the Application

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or run the `FlippaCloneApplication` class from your IDE.

### 5. Access the Application

- **Application**: http://localhost
- **H2 Console**: http://localhost/h2-console
  - JDBC URL: `jdbc:h2:file:./data/flippa_db`
  - Username: `sa`
  - Password: (leave empty)

### 6. Default Admin Credentials

- **Email/Username**: admin@flippa.com or admin
- **Password**: password

**Test User**:
- **Email**: test@flippa.com
- **Password**: password

**⚠️ IMPORTANT**: Change the default passwords immediately in production!

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/flippa/
│   │   ├── entity/          # JPA entities
│   │   ├── repository/      # Data access layer
│   │   ├── dto/             # Data transfer objects
│   │   ├── service/         # Business logic layer
│   │   ├── controller/      # Web controllers
│   │   ├── security/        # Security configuration
│   │   └── FlippaCloneApplication.java
│   └── resources/
│       ├── db/migration/    # Flyway migrations
│       ├── templates/       # Thymeleaf templates
│       └── application.yml  # Application configuration
└── test/                    # Test files
```

## 🗄️ Database Schema

The application uses Flyway for database migrations. Key tables include:

- **users**: User accounts with role-based access
- **roles**: System roles (USER, SELLER, BUYER, ADMIN, SUPER_ADMIN)
- **listings**: Business listings (websites, social accounts, domains)
- **website_info**: Automatically fetched website analytics
- **escrows**: Escrow transactions
- **payments**: Payment records
- **system_configs**: System configuration settings
- **audit_logs**: Complete audit trail

## 🔐 Security

- **Authentication**: Spring Security with form-based login
- **Authorization**: Role-based access control (RBAC)
- **Password Encryption**: BCrypt password hashing
- **CSRF Protection**: Enabled for all forms
- **Session Management**: Secure session handling

## 💳 Payment Gateways

### PayPal Integration
- Supports both sandbox and live modes
- Configure client ID and secret via admin UI (`/admin/settings`)
- Payment callbacks handled automatically

### PayNow Zimbabwe Integration
- Uses official PayNow Java SDK (https://github.com/paynow/Paynow-Java-SDK)
- Configure integration ID and key via admin UI (`/admin/settings`)
- Supports web-based transactions
- Automatic payment status polling using poll URL
- Secure hash verification handled by SDK
- Supports return and result URL callbacks
- Payment status checked automatically when viewing payment details

## 🔄 Continuous Development

The application supports continuous development through:

1. **Flyway Migrations**: All database changes are versioned
2. **Structured Logging**: All processes are logged for debugging
3. **Configuration Management**: System settings can be toggled without code changes
4. **Audit Trail**: Complete history of all system actions

### Adding New Migrations

Create new migration files in `src/main/resources/db/migration/`:

```
V3__Add_new_feature.sql
V4__Update_schema.sql
```

Flyway will automatically apply migrations on application startup.

## 📊 Website Info Fetching

The application automatically fetches website information when a listing is created with a website URL:

- **Domain Extraction**: Extracts domain from URL
- **Platform Detection**: Detects platform (WordPress, Shopify, etc.)
- **Traffic Data**: Fetches traffic information (when APIs are integrated)
- **Revenue Data**: Fetches revenue information (when APIs are integrated)

**Note**: For production, integrate with:
- Google Analytics API
- SimilarWeb API
- Moz/Ahrefs APIs for domain authority
- Revenue tracking APIs

## 🧪 Testing

Run tests with:

```bash
mvn test
```

## 📝 API Endpoints

### Public Endpoints
- `GET /` - Home page
- `GET /listings` - Browse listings
- `GET /listings/{id}` - View listing details
- `GET /register` - Registration page
- `GET /login` - Login page

### Authenticated Endpoints
- `GET /my-listings` - User's listings
- `POST /my-listings/create` - Create listing
- `GET /escrow/my-escrows` - User's escrows
- `POST /escrow/create` - Create escrow
- `POST /payment/initiate` - Initiate payment

### Admin Endpoints
- `GET /admin` - Admin dashboard
- `GET /admin/users` - User management
- `GET /admin/listings` - Listing management
- `GET /admin/disputes` - Dispute management
- `GET /admin/settings` - System settings

## 🚀 Deployment

### Production Checklist

1. **Change Default Admin Password**
2. **Update JWT Secret**: Use a strong, random secret key
3. **Configure Payment Gateways**: Add production credentials
4. **Database**: Consider migrating from H2 to PostgreSQL or MySQL
5. **SSL/HTTPS**: Enable HTTPS in production
6. **Environment Variables**: Use environment variables for sensitive data
7. **Logging**: Configure proper log rotation and storage
8. **Backup**: Set up database backups

### Environment Variables

Set these environment variables in production:

```bash
JWT_SECRET=your-production-secret-key
PAYPAL_CLIENT_ID=your-paypal-client-id
PAYPAL_CLIENT_SECRET=your-paypal-secret
PAYPAL_MODE=live
PAYNOW_INTEGRATION_ID=your-paynow-id
PAYNOW_INTEGRATION_KEY=your-paynow-key
```

## 🤝 Contributing

This is an open-source project. Contributions are welcome!

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

This project is open source and available under the MIT License.

## 👥 Authors

- **DexterWura** - Initial work

## 🙏 Acknowledgments

- Inspired by Flippa.com
- Built with Spring Boot and modern Java technologies

## 📞 Support

For issues and questions, please open an issue on GitHub.

---

**Note**: This is a production-grade application template. Ensure proper security measures, testing, and configuration before deploying to production.
