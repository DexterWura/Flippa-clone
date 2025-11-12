# Unit Tests

This directory contains comprehensive unit tests for the Flippa Clone application using JUnit 5 and Mockito.

## Test Coverage

### Services Tested

1. **UserServiceTest** - Tests user registration, authentication, banning/unbanning, and role management
   - User registration with email validation
   - Finding users by email/ID
   - Banning and unbanning users
   - Toggling user roles
   - Error handling for invalid operations

2. **ListingServiceTest** - Tests listing creation, updates, activation, and search
   - Creating listings with auto-approve enabled/disabled
   - Website info auto-fetching
   - Listing updates with authorization checks
   - Admin activation of listings
   - Search and filtering functionality
   - DTO conversion

3. **PaymentServiceTest** - Tests payment initiation, processing, and status checks
   - PayPal payment initiation
   - PayNow Zim payment initiation
   - Payment callback processing
   - Payment status checking
   - Gateway enablement validation
   - Authorization checks

4. **EscrowServiceTest** - Tests escrow creation, disputes, and transfer completion
   - Escrow creation with validation
   - Dispute raising and resolution
   - Transfer completion
   - Authorization checks for buyers/sellers
   - DTO conversion

5. **AdminServiceTest** - Tests system configuration management
   - Config retrieval and updates
   - Config toggling
   - Payment gateway configuration
   - System name retrieval
   - Auto-approve configuration

## Running Tests

Run all tests:
```bash
mvn test
```

Run specific test class:
```bash
mvn test -Dtest=UserServiceTest
```

Run with coverage:
```bash
mvn test jacoco:report
```

## Test Structure

All tests follow the Arrange-Act-Assert pattern:
- **Arrange**: Set up mocks and test data
- **Act**: Execute the method under test
- **Assert**: Verify the results and interactions

## Mocking Strategy

- Repository methods are mocked to avoid database dependencies
- External services (PayPal, PayNow) are mocked
- HTTP requests are mocked
- Audit logging is verified but not executed

## Future Test Additions

- Controller integration tests
- Repository tests with in-memory database
- Security configuration tests
- Validation tests
- End-to-end workflow tests

