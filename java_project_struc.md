
```plaintext
my-app/
├── pom.xml                             # Maven build configuration file (dependencies, plugins, etc.)
├── .gitignore                          # Specifies intentionally untracked files to ignore in Git
├── README.md                           # Project overview, usage, and documentation

├── src/                                # Source root
│   ├── main/                           # Main production source set
│   │   ├── java/                       # Java source files
│   │   │   └── com/
│   │   │       └── mycompany/
│   │   │           └── myapp/
│   │   │               ├── MyAppApplication.java          # Entry point for the application (Spring Boot @SpringBootApplication)
│   │   │
│   │   │               ├── config/                        # Application-level configuration classes
│   │   │               │   └── WebSecurityConfig.java     # Defines Spring Security rules (authentication, CORS, etc.)
│   │   │
│   │   │               ├── domain/                        # Domain layer (core business logic and models)
│   │   │               │   ├── model/                     # Entity and Value Object definitions
│   │   │               │   │   └── User.java              # Business entity representing a user
│   │   │               │   ├── repository/                # Domain repository interfaces (no JPA here)
│   │   │               │   │   └── UserRepository.java    # Interface for accessing User data (abstraction)
│   │   │               │   └── service/                   # Domain-level services (pure logic)
│   │   │               │       └── UserDomainService.java # Implements domain business rules (no infrastructure access)
│   │   │
│   │   │               ├── application/                   # Application logic layer (use cases, commands, coordination)
│   │   │               │   ├── service/                   # Application services calling domain + orchestrating logic
│   │   │               │   │   └── UserApplicationService.java  # Handles business workflows and invokes domain service
│   │   │               │   ├── dto/                       # DTOs (Data Transfer Objects) for communication between layers
│   │   │               │   │   └── UserDto.java           # Output data structure for transferring user info
│   │   │               │   └── command/                   # Commands (write requests) sent from clients to services
│   │   │               │       └── CreateUserCommand.java # Represents the input to create a new user
│   │   │
│   │   │               ├── infrastructure/                # Concrete implementations of external systems
│   │   │               │   ├── repository/                # Repository implementations (e.g., JPA-based)
│   │   │               │   │   └── JpaUserRepository.java # JPA-based implementation of UserRepository
│   │   │               │   ├── config/                    # Infrastructure-related configurations (DB, caching)
│   │   │               │   │   └── DatabaseConfig.java    # Configures database connection and JPA settings
│   │   │               │   └── external/                  # Integration with external services (REST APIs, etc.)
│   │   │               │       └── RestUserClient.java    # REST client for communicating with an external user service
│   │   │
│   │   │               ├── api/                           # API layer (controllers)
│   │   │               │   └── controller/                # HTTP endpoint handlers
│   │   │               │       └── UserController.java    # REST controller handling user-related requests
│   │   │
│   │   │               └── util/                          # Utility classes and helper functions
│   │   │                   └── DateUtils.java             # Utility for date formatting and parsing

│   │   └── resources/                                     # Configuration and resource files
│   │       ├── application.yml                            # Main Spring Boot configuration (app settings, DB, ports)
│   │       ├── logback.xml                                # Logging configuration for Logback (used by default in Spring Boot)
│   │       └── db/
│   │           └── migration/                             # SQL migration scripts (used by Flyway or Liquibase)
│   │               └── V1__init_schema.sql                # Initial schema creation SQL script

│   └── test/                                              # Test source set
│       ├── java/                                          # Unit and integration test code
│       │   └── com/
│       │       └── mycompany/
│       │           └── myapp/
│       │               ├── domain/
│       │               │   └── service/
│       │               │       └── UserDomainServiceTest.java   # Unit test for UserDomainService
│       │               └── api/
│       │                   └── controller/
│       │                       └── UserControllerTest.java      # Unit/integration test for UserController
│       └── resources/
│           └── test-data.sql                              # SQL script to seed data for integration tests

cryptography/
└── src/
    └── main/java/com/myorg/crypto/
        ├── service/             --> Public services (e.g., CryptoService)
        ├── impl/                --> Implementation details
        ├── config/              --> Configuration files or beans
        ├── exceptions/          --> Custom exceptions
        └── utils/               --> Low-level utilities

📁 src
├── 📁 main
│   ├── 📁 java
│   │   └── 📁 com
│   │       └── 📁 example
│   │           └── 📁 crypto
│   │               ├── 📁 config
│   │               │   └── CryptoConfig.java
│   │               ├── 📁 controller
│   │               │   └── CryptoController.java
│   │               ├── 📁 service
│   │               │   └── CryptoService.java
│   │               ├── 📁 core
│   │               │   ├── CipherFactory.java
│   │               │   ├── IPasswordCipher.java
│   │               │   ├── AESCBCPasswordCipher.java
│   │               │   ├── AESECBPasswordCipher.java
│   │               ├── 📁 utils
│   │               │   └── CipherUtils.java
│   │               └── CryptoApplication.java
│   └── 📁 resources
│       ├── application.yml
