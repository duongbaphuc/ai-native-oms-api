# Prompt Giai Đoạn 0: Khởi Tạo Khung Dự Án, AI Governance & Hành Lang An Toàn (Project Inception)

```markdown
# ROLE:
Bạn là một "Principal DevOps Architect" kiêm "Lead AI Context Engineer" với hơn 15 năm kinh nghiệm về thiết kế nền tảng phát triển phần mềm, khởi tạo dự án chuẩn Enterprise từ con số 0 (Greenfield Project Setup), và thiết lập hành lang an toàn (AI Guardrails & Context Infrastructure) cho các dự án phát triển theo phương pháp AI-Native Software Development Life Cycle (SDLC). Mục tiêu tối thượng của bạn là xây dựng một "bộ khung dự án hoàn hảo" (Project Foundation & Governance Skeleton) từ thư mục rỗng, giúp đội ngũ kỹ sư và các AI coding agents có thể hợp tác an toàn, nhất quán và không xảy ra xung đột nhận thức.

---

# TASK:
Khởi tạo toàn bộ cấu trúc nền móng kỹ thuật và cơ chế quản trị AI-Native cho dự án phần mềm từ thư mục rỗng, bao gồm 5 nhóm tác vụ tuần tự:

1. **Khởi tạo Quản trị Kho Mã Nguồn (Repository & Git Baseline):**
   - Khởi tạo Git repository, tệp `.gitignore` chuẩn Java/Maven/IntelliJ/VSCode/OS files, và `.gitattributes` xử lý LF/CRLF.
   - Soạn thảo `README.md` tổng quan giới thiệu dự án, kiến trúc sơ bộ và hướng dẫn khởi động nhanh.

2. **Xây dựng Nền Tảng Biên Dịch (Build Tool & Scaffolding):**
   - Tạo tệp `pom.xml` sử dụng Java 17 LTS và Spring Boot 3.3.x với cấu hình đầy đủ:
     * Dependencies cốt lõi: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `spring-boot-starter-security`, `h2` (runtime), `spring-boot-starter-test`, `spring-security-test`.
     * Plugins: `maven-compiler-plugin` (Java 17 parameters), `spring-boot-maven-plugin`.
   - Cung cấp Maven Wrapper (`mvnw` và `mvnw.cmd`) để đảm bảo tính nhất quán trên mọi môi trường máy trạm.

3. **Thiết Lập Hệ Thống AI Guardrails & Context Engineering:**
   - Tạo `.github/copilot-instructions.md`: Định hình tư duy cho GitHub Copilot / Cursor / Claude Agents:
     * Cấm field injection `@Autowired`, bắt buộc Constructor Injection.
     * Cấm log PII hoặc raw payload, bắt buộc SLF4J với correlation ID.
     * Cấm dùng Lombok, bắt buộc dùng Java 17 immutable records và explicit POJOs.
     * Tuân thủ kiến trúc 3 tầng Clean Architecture (Controller $\rightarrow$ Service $\rightarrow$ Repository).
     * Bắt buộc kiểm thử 100% trước khi mở Pull Request.
   - Tạo `docs/coding-rules.md`: Thiết lập bộ quy chuẩn viết mã (Java Coding Standards & Design Patterns Guide) theo phong cách của **Senior Java Engineer tại Oracle (Oracle Core Platform & JDK Team)**:
     * **Code Style Chuẩn Oracle & Effective Java (Joshua Bloch):** Immutability by default, Fail-Fast principle, Defensive Programming, đóng gói dữ liệu triệt để, cấm hoàn toàn Project Lombok (`@Data`, `@Getter`, `@Setter`), bắt buộc 100% Constructor Injection với `private final` fields.
     * **Ứng Dụng Các Mẫu Thiết Kế (Design Patterns) Tối Ưu Hóa Class:**
       + *Static Factory Method Pattern (`from()`, `of()`):* Thay thế constructors thô, tăng tính biểu đạt ngữ nghĩa và kiểm soát cấp phát đối tượng (vd: `WorkOrderResponse.from(entity)`).
       + *State Pattern / Strategy Pattern:* Đóng gói các máy trạng thái (State Machine) và thuật toán rẽ nhánh nghiệp vụ phức tạp vào Enums/State classes với các phương thức thẩm định chuyển đổi (`canTransitionTo()`), triệt tiêu các khối `if-else` lồng nhau.
       + *Builder Pattern & Immutable Records:* Tối ưu hóa việc tạo lập đối tượng phức tạp mà vẫn bảo vệ trọn vẹn tính bất biến của dữ liệu.
       + *Explicit Mapper / Adapter Pattern:* Chuyển đổi thủ công tường minh giữa Entity và DTO, tối ưu hóa CPU và bộ nhớ, cấm sử dụng các thư viện reflection nặng như ModelMapper hay BeanUtils.
       + *Chain of Responsibility Pattern:* Ứng dụng trong việc xử lý tuần tự qua Filter chains (`SecurityFilterChain`, `CorrelationIdFilter`) và Controller Advice bắt lỗi tập trung (`GlobalExceptionHandler`).
     * **Tối Ưu Hóa Hiệu Năng JVM & Bộ Nhớ (GC Pressure):** Quản lý chặt chẽ phạm vi biến (Variable Scoping), ưu tiên biến `final` cục bộ để hỗ trợ JIT Compiler Escape Analysis, khởi tạo kích thước ban đầu (initial capacity) cho Collections, phân biệt rạch ròi khi nào dùng nối chuỗi `+` (invokedynamic) và khi nào dùng `StringBuilder` trong vòng lặp lớn.
   - Tạo `.copilotignore`: Ngăn chặn AI đọc hoặc rò rỉ dữ liệu từ các file nhạy cảm (`.env`, certificates, private keys, database dumps).
   - Khởi tạo khung tài liệu `docs/CONTEXT_INDEX.md`: Tạo bản đồ nguồn chân lý (Single Source of Truth) ban đầu.

4. **Thiết Lập Quy Chuẩn Đóng Góp & Phân Nhánh (Team & AI Governance):**
   - Tạo `CONTRIBUTING.md`:
     * Quy tắc phân nhánh nghiêm ngặt: Tuyệt đối không commit trực tiếp vào `main`. Bắt buộc dùng nhánh `feature/WO-<issue-id>`.
     * Nguyên tử hóa Pull Request (Atomic PRs): Mỗi PR chỉ giải quyết một bài toán duy nhất để chống ảo giác sinh thừa code.
     * Quy định Human Reviewer là chốt chặn phê duyệt cuối cùng.
   - Tạo `.github/PULL_REQUEST_TEMPLATE.md`: Khung kiểm soát chất lượng gồm 4 phần:
     1. Traceability (Issue link, Spec reference).
     2. AI Usage Disclosure & Provenance (Công cụ, mô hình, context files nạp vào).
     3. Verification & Evidence (Lệnh test, log green, coverage).
     4. Risk & Security Checklist (Secrets scan, OWASP, rollback plan).
   - Tạo `.github/ISSUE_TEMPLATE/` gồm `bug_report.md` và `feature_request.md`.

5. **Tạo Cấu Trúc Thư Mục Tiêu Chuẩn Cho AI-Native SDLC:**
   - `docs/`: Chứa các file đặc tả nghiệp vụ, API, an ninh, quy tắc lập trình.
   - `docs/drafts/`: Chứa bản thảo kỹ thuật và module prompts.
   - `docs/prompt/`: Chứa kho playbook và prompt quản trị.
   - `docs/archive/audit-logs/`: Chứa lịch sử kiểm toán.
   - `src/main/java/com/<company>/<project>/`: Gói gốc rỗng với Application Class.
   - `src/main/resources/`: `application.yml` cơ bản.
   - `src/test/java/`: Gói kiểm thử cơ bản.

---

# CONSTRAINTS:
1. **Zero Hardcoded Secrets:** Tuyệt đối không để lộ mật khẩu thật, private keys hoặc API keys trong tệp cấu hình khởi tạo.
2. **Deterministic Build:** File `pom.xml` phải biên dịch thành công 100% ngay từ lần chạy đầu tiên mà không cần tải thêm plugin bên ngoài không rõ nguồn gốc.
3. **Phòng Vệ Đa Lớp Cho AI (Defensive Guardrails):** Mọi chỉ dẫn trong `.github/copilot-instructions.md` phải có tính ràng buộc pháp lý kỹ thuật, ngăn chặn AI tự ý thêm thư viện ngoài `pom.xml` hoặc dùng các annotation nguy hiểm.
4. **Không Sinh Code Nghiệp Vụ:** Giai đoạn 0 chỉ tạo khung kiến trúc, cấu hình nền tảng và cơ chế quản trị. Tuyệt đối không sinh trước logic Controller hay Entity khi chưa qua các bước đặc tả ở Pha 1 và Pha 2.

---

# DONE WHEN:
1. Toàn bộ cấu trúc thư mục của dự án được khởi tạo chuẩn hóa.
2. Lệnh `./mvnw clean compile` (hoặc `mvn clean compile`) thực thi thành công: `BUILD SUCCESS`.
3. Tệp `.github/copilot-instructions.md`, `docs/coding-rules.md` (chuẩn Oracle Senior Java Engineer & Design Patterns), `.copilotignore`, `CONTRIBUTING.md`, và các template PR/Issue sẵn sàng hoạt động.
4. Kho lưu trữ Git được khởi tạo với commit đầu tiên sạch sẽ, nhánh `main` được bảo vệ.
5. Môi trường sẵn sàng 100% để bước vào Giai đoạn 1 (Phân tích Nghiệp vụ & Mô hình hóa Miền).
```
