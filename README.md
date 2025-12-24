# Mini-Drive Storage Platform

A full-stack file storage and sharing platform built with Spring Boot and Next.js. This system provides secure file management with folder hierarchies, permission-based sharing, and asynchronous folder downloads.

## Description

Mini-Drive is a cloud storage solution that handles file uploads, folder organization, and collaborative sharing. The backend uses Spring Boot 4 with JWT authentication, while the frontend is built with Next.js 16 and React 19. The platform supports multi-file uploads, recursive folder permissions, and async ZIP generation for folder downloads.

## Technical Highlights

### Backend Techniques

- **[JWT Authentication](https://developer.mozilla.org/en-US/docs/Web/HTTP/Authentication)** with stateless session management using Spring Security filters
- **[@Async Processing](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)** for folder ZIP creation and email notifications
- **[@Scheduled](https://docs.spring.io/spring-framework/reference/integration/scheduling.html) Tasks** with multi-threaded cleanup using ExecutorService
- **Method-level Security** using [@PreAuthorize](https://docs.spring.io/spring-security/reference/servlet/authorization/method-security.html) annotations
- **Polling Pattern** for long-running operations with status tracking
- **Self-referencing Foreign Keys** for hierarchical folder structures
- **Unified Resource Handling** - single endpoint manages both files and folders via content type negotiation

### Frontend Techniques

- **[Context API](https://react.dev/reference/react/createContext)** for authentication state management
- **[Blob API](https://developer.mozilla.org/en-US/docs/Web/API/Blob)** for client-side file downloads
- **[FormData API](https://developer.mozilla.org/en-US/docs/Web/API/FormData)** for multi-file uploads
- **[setTimeout Polling](https://developer.mozilla.org/en-US/docs/Web/API/setTimeout)** for async download status checks
- **[localStorage](https://developer.mozilla.org/en-US/docs/Web/API/Window/localStorage)** for JWT token persistence
- **Type-safe API Client** with custom error handling

## Technologies & Libraries

### Backend Stack
- [Spring Boot 4.0](https://spring.io/projects/spring-boot) - Application framework
- [Spring Security](https://spring.io/projects/spring-security) - Authentication & authorization
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa) - Data persistence
- [JJWT 0.12.6](https://github.com/jwtk/jjwt) - JWT token generation and validation
- [Lombok](https://projectlombok.org/) - Boilerplate code reduction
- [H2](https://www.h2database.com/) / [PostgreSQL](https://www.postgresql.org/) - Database options
- [BCrypt](https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/crypto/bcrypt/BCryptPasswordEncoder.html) - Password hashing
- Java 21 with Maven

### Frontend Stack
- [Next.js 16](https://nextjs.org/) - React framework with App Router
- [React 19](https://react.dev/) - UI library
- [TypeScript 5](https://www.typescriptlang.org/) - Type safety
- [Tailwind CSS 4](https://tailwindcss.com/) - Utility-first styling
- [shadcn/ui](https://ui.shadcn.com/) - Component library
- [@base-ui/react](https://www.npmjs.com/package/@base-ui/react) - Base UI primitives
- [Lucide React](https://lucide.dev/) - Icon library
- [Sonner](https://sonner.emilkowal.ski/) - Toast notifications
- [next-themes](https://github.com/pacocoursey/next-themes) - Theme management
- [Public Sans](https://fonts.google.com/specimen/Public+Sans) - Google font by USWDS

## Project Structure

```
mini-drive-storage-be/
├── src/
│   ├── main/
│   │   ├── java/org/fyp/minidrivestoragebe/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── entity/
│   │   │   ├── enums/
│   │   │   ├── exception/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   └── test/
├── storage/
└── pom.xml

mini-drive-storage-fe/
├── app/
│   ├── dashboard/
│   ├── login/
│   └── register/
├── components/
│   └── ui/
├── lib/
│   └── api/
├── public/
└── package.json

```

**[mini-drive-storage-be/](mini-drive-storage-be/)** - Spring Boot REST API with layered architecture (controller → service → repository)

**[mini-drive-storage-fe/](mini-drive-storage-fe/)** - Next.js frontend with App Router and client-side state management

**[mini-drive-storage-be/src/main/java/org/fyp/minidrivestoragebe/config/](mini-drive-storage-be/src/main/java/org/fyp/minidrivestoragebe/config/)** - JWT utilities and async task executor configuration

**[mini-drive-storage-be/src/main/java/org/fyp/minidrivestoragebe/security/](mini-drive-storage-be/src/main/java/org/fyp/minidrivestoragebe/security/)** - JWT filter chain and custom UserDetailsService implementation

**[mini-drive-storage-be/storage/](mini-drive-storage-be/storage/)** - File system storage for uploaded files

**[mini-drive-storage-fe/app/dashboard/](mini-drive-storage-fe/app/dashboard/)** - Main file management interface with search and filtering

**[mini-drive-storage-fe/lib/api/](mini-drive-storage-fe/lib/api/)** - Type-safe API client with error handling

**[mini-drive-storage-fe/components/](mini-drive-storage-fe/components/)** - Reusable React components including sharing dialog
