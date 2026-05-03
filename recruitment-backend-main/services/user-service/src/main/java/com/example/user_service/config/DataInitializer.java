package com.example.user_service.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.example.user_service.model.Department;
import com.example.user_service.model.Employee;
import com.example.user_service.model.Position;
import com.example.user_service.model.Role;
import com.example.user_service.model.Permission;
import com.example.user_service.model.User;
import com.example.user_service.repository.DepartmentRepository;
import com.example.user_service.repository.EmployeeRepository;
import com.example.user_service.repository.PositionRepository;
import com.example.user_service.repository.RoleRepository;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.repository.PermissionRepository;

@Component
public class DataInitializer implements CommandLineRunner {
        private static final Logger logger = Logger.getLogger(DataInitializer.class.getName());

        // Mật khẩu '123456' đã được mã hóa bằng BCrypt
        private static final String BCRYPTED_PASSWORD = "$2a$10$iM2Qd9bsZBZxHwmutzqyLOA1u7cGCK92xE2XG6zejUyDCWuZZbgLu";

        private final DepartmentRepository departmentRepository;
        private final PositionRepository positionRepository;
        private final EmployeeRepository employeeRepository;
        private final RoleRepository roleRepository;
        private final UserRepository userRepository;
        private final PermissionRepository permissionRepository;

        public DataInitializer(DepartmentRepository departmentRepository, PositionRepository positionRepository,
                        EmployeeRepository employeeRepository, RoleRepository roleRepository,
                        UserRepository userRepository, PermissionRepository permissionRepository) {
                this.departmentRepository = departmentRepository;
                this.positionRepository = positionRepository;
                this.employeeRepository = employeeRepository;
                this.roleRepository = roleRepository;
                this.userRepository = userRepository;
                this.permissionRepository = permissionRepository;
        }

        @Override
        public void run(String... args) throws Exception {
                if (userRepository.count() > 0) {
                        logger.info("Dữ liệu đã tồn tại. Bỏ qua quá trình khởi tạo.");
                        return;
                }

                logger.info("Bắt đầu quá trình khởi tạo dữ liệu mẫu...");

                // =====================================================================================
                // 1. TẠO PHÒNG BAN (DEPARTMENTS)
                // =====================================================================================
                Department directorBoard = new Department();
                directorBoard.setCode("BOARD");
                directorBoard.setName("Ban Giám đốc");
                directorBoard.setDescription("Chịu trách nhiệm quản lý và điều hành chung toàn bộ công ty.");
                directorBoard.set_active(true);

                Department hrDepartment = new Department();
                hrDepartment.setCode("HR");
                hrDepartment.setName("Phòng Nhân sự");
                hrDepartment.setDescription(
                                "Chịu trách nhiệm về các vấn đề liên quan đến con người, bao gồm tuyển dụng, đào tạo và chính sách.");
                hrDepartment.set_active(true);

                Department salesDepartment = new Department();
                salesDepartment.setCode("SALES");
                salesDepartment.setName("Phòng Kinh doanh");
                salesDepartment
                                .setDescription("Phụ trách các hoạt động bán hàng, tìm kiếm và duy trì mối quan hệ với khách hàng.");
                salesDepartment.set_active(true);

                Department techDepartment = new Department();
                techDepartment.setCode("TECH");
                techDepartment.setName("Phòng Kỹ thuật");
                techDepartment.setDescription(
                                "Phát triển, bảo trì và vận hành các sản phẩm, hệ thống công nghệ của công ty.");
                techDepartment.set_active(true);

                Department marketingDepartment = new Department();
                marketingDepartment.setCode("MKT");
                marketingDepartment.setName("Phòng Marketing");
                marketingDepartment
                                .setDescription("Xây dựng và thực hiện các chiến lược tiếp thị, quảng bá thương hiệu và sản phẩm.");
                marketingDepartment.set_active(true);

                Department accountingDepartment = new Department();
                accountingDepartment.setCode("ACC");
                accountingDepartment.setName("Phòng Kế toán");
                accountingDepartment.setDescription("Quản lý các vấn đề tài chính, kế toán và thuế của công ty.");
                accountingDepartment.set_active(true);

                departmentRepository.saveAll(Arrays.asList(directorBoard, hrDepartment, salesDepartment, techDepartment,
                                marketingDepartment, accountingDepartment));
                logger.info("Đã tạo " + departmentRepository.count() + " phòng ban.");

                // =====================================================================================
                // 1.1 TẠO VỊ TRÍ (POSITIONS)
                // =====================================================================================
                Position ceoPosition = new Position();
                ceoPosition.setName("Giám đốc điều hành");
                ceoPosition.setLevel("C-Level");
                ceoPosition.setHierarchyOrder(1);
                ceoPosition.setActive(true);

                Position managerPosition = new Position();
                managerPosition.setName("Trưởng phòng");
                managerPosition.setLevel("Manager");
                managerPosition.setHierarchyOrder(2);
                managerPosition.setActive(true);

                Position seniorStaffPosition = new Position();
                seniorStaffPosition.setName("Nhân viên cấp cao");
                seniorStaffPosition.setLevel("Senior");
                seniorStaffPosition.setHierarchyOrder(3);
                seniorStaffPosition.setActive(true);

                Position staffPosition = new Position();
                staffPosition.setName("Nhân viên");
                staffPosition.setLevel("Staff");
                staffPosition.setHierarchyOrder(4);
                staffPosition.setActive(true);

                Position adminPosition = new Position();
                adminPosition.setName("Quản trị viên");
                adminPosition.setLevel("Admin");
                adminPosition.setHierarchyOrder(0);
                adminPosition.setActive(true);

                positionRepository.saveAll(Arrays.asList(ceoPosition, managerPosition, seniorStaffPosition,
                                staffPosition, adminPosition));
                logger.info("Đã tạo " + positionRepository.count() + " vị trí.");

                // =====================================================================================
                // 2. TẠO VAI TRÒ (ROLES)
                // =====================================================================================
                Role adminRole = new Role();
                adminRole.setName("ADMIN");
                adminRole.setDescription("Quản trị viên hệ thống, có quyền cao nhất.");
                adminRole.set_active(true);

                Role ceoRole = new Role();
                ceoRole.setName("CEO");
                ceoRole.setDescription(
                                "Giám đốc điều hành, người duyệt cuối cùng các yêu cầu và kế hoạch tuyển dụng quan trọng.");
                ceoRole.set_active(true);

                Role managerRole = new Role();
                managerRole.setName("MANAGER");
                managerRole.setDescription("Trưởng phòng, người đề xuất nhu cầu tuyển dụng và duyệt hồ sơ ứng viên.");
                managerRole.set_active(true);

                Role staffRole = new Role();
                staffRole.setName("STAFF");
                staffRole.setDescription("Nhân viên, người thực hiện các tác vụ chuyên môn.");
                staffRole.set_active(true);

                logger.info("Đã tạo " + 4 + " vai trò.");

                // =====================================================================================
                // 2.1 TẠO QUYỀN (PERMISSIONS) THEO QUY ƯỚC <service>:<resource>:<action>
                // Format mới: chỉ có 2 action: "read" và "manage"
                // GET → "read", POST/PUT/PATCH/DELETE và các endpoint đặc biệt → "manage"
                // =====================================================================================
                List<String> permissionNames = Arrays.asList(
                                // user-service permissions
                                "user-service:permissions:read", "user-service:permissions:manage",
                                "user-service:roles:read", "user-service:roles:manage",
                                "user-service:departments:read", "user-service:departments:manage",
                                "user-service:employees:read", "user-service:employees:manage",
                                "user-service:positions:read", "user-service:positions:manage",
                                "user-service:users:read", "user-service:users:manage",
                                // job-service permissions
                                "job-service:job-positions:read", "job-service:job-positions:manage",
                                "job-service:recruitment-requests:read", "job-service:recruitment-requests:manage",
                                "job-service:job-skills:read", "job-service:job-skills:manage",
                                "job-service:job-categories:read", "job-service:job-categories:manage",
                                // candidate-service permissions
                                // Note: applications permissions kept for backward compatibility (Application
                                // merged into Candidate)
                                "candidate-service:applications:read", "candidate-service:applications:manage",
                                "candidate-service:candidates:read", "candidate-service:candidates:manage",
                                "candidate-service:comments:read", "candidate-service:comments:manage",
                                // communications-service permissions
                                "communications-service:schedules:read", "communications-service:schedules:manage",
                                "communications-service:notifications:read",
                                "communications-service:notifications:manage",
                                // workflow-service permissions
                                "workflow-service:workflows:read", "workflow-service:workflows:manage",
                                "workflow-service:approval-trackings:read",
                                "workflow-service:approval-trackings:manage",
                                // notification-service permissions
                                "notification-service:notifications:read", "notification-service:notifications:manage",
                                // email-service permissions
                                "email-service:emails:read", "email-service:emails:manage");

                List<Permission> savedPermissions = permissionRepository
                                .saveAll(permissionNames.stream().map(name -> {
                                        Permission p = new Permission();
                                        p.setName(name);
                                        p.setActive(true);
                                        return p;
                                }).toList());

                Map<String, Permission> permissionMap = savedPermissions.stream()
                                .collect(Collectors.toMap(Permission::getName, p -> p));
                Function<String, Permission> requirePermission = name -> {
                        Permission p = permissionMap.get(name);
                        if (p == null) {
                                throw new IllegalStateException("Permission not found: " + name);
                        }
                        return p;
                };

                // Gán quyền cho từng vai trò
                // ADMIN: Tất cả quyền (read + manage)
                adminRole.setPermissions(new HashSet<>(savedPermissions));

                // CEO: Đọc tất cả, manage một số quan trọng
                ceoRole.setPermissions(new HashSet<>(List.of(
                                // user-service - đọc tất cả
                                requirePermission.apply("user-service:permissions:read"),
                                requirePermission.apply("user-service:roles:read"),
                                requirePermission.apply("user-service:departments:read"),
                                requirePermission.apply("user-service:employees:read"),
                                requirePermission.apply("user-service:positions:read"),
                                requirePermission.apply("user-service:users:read"),
                                // job-service - đọc tất cả, manage vị trí và yêu cầu tuyển dụng
                                requirePermission.apply("job-service:job-positions:read"),
                                requirePermission.apply("job-service:job-positions:manage"),
                                requirePermission.apply("job-service:recruitment-requests:read"),
                                requirePermission.apply("job-service:recruitment-requests:manage"),
                                requirePermission.apply("job-service:job-skills:read"),
                                requirePermission.apply("job-service:job-categories:read"),
                                // candidate-service - đọc tất cả, manage candidates
                                requirePermission.apply("candidate-service:candidates:read"),
                                requirePermission.apply("candidate-service:comments:read"),
                                // communications-service - đọc và quản lý lịch
                                requirePermission.apply("communications-service:schedules:read"),
                                requirePermission.apply("communications-service:schedules:manage"),
                                requirePermission.apply("communications-service:notifications:read"),
                                // workflow-service - đọc và phê duyệt
                                requirePermission.apply("workflow-service:workflows:read"),
                                requirePermission.apply("workflow-service:approval-trackings:read"),
                                requirePermission.apply("workflow-service:approval-trackings:manage"),
                                // notification-service - đọc
                                requirePermission.apply("notification-service:notifications:read"),
                                // email-service - đọc
                                requirePermission.apply("email-service:emails:read"))));

                // MANAGER: Đọc nhiều, manage các resources liên quan đến công việc của họ
                managerRole.setPermissions(new HashSet<>(List.of(
                                // user-service - đọc, manage employees
                                requirePermission.apply("user-service:departments:read"),
                                requirePermission.apply("user-service:employees:read"),
                                requirePermission.apply("user-service:employees:manage"),
                                requirePermission.apply("user-service:positions:read"),
                                requirePermission.apply("user-service:users:read"),
                                // job-service - đọc và manage tất cả
                                requirePermission.apply("job-service:job-positions:read"),
                                requirePermission.apply("job-service:job-positions:manage"),
                                requirePermission.apply("job-service:recruitment-requests:read"),
                                requirePermission.apply("job-service:recruitment-requests:manage"),
                                requirePermission.apply("job-service:job-skills:read"),
                                requirePermission.apply("job-service:job-categories:read"),
                                // candidate-service - đọc và manage tất cả (applications merged into
                                // candidates)
                                requirePermission.apply("candidate-service:candidates:read"),
                                requirePermission.apply("candidate-service:candidates:manage"),
                                requirePermission.apply("candidate-service:comments:read"),
                                requirePermission.apply("candidate-service:comments:manage"),
                                // communications-service - đọc và manage lịch
                                requirePermission.apply("communications-service:schedules:read"),
                                requirePermission.apply("communications-service:schedules:manage"),
                                requirePermission.apply("communications-service:notifications:read"),
                                requirePermission.apply("communications-service:notifications:manage"),
                                // workflow-service - đọc và manage
                                requirePermission.apply("workflow-service:workflows:read"),
                                requirePermission.apply("workflow-service:workflows:manage"),
                                requirePermission.apply("workflow-service:approval-trackings:read"),
                                requirePermission.apply("workflow-service:approval-trackings:manage"),
                                // notification-service - đọc và manage
                                requirePermission.apply("notification-service:notifications:read"),
                                requirePermission.apply("notification-service:notifications:manage"),
                                // email-service - đọc
                                requirePermission.apply("email-service:emails:read"))));

                // STAFF: Chỉ read
                staffRole.setPermissions(new HashSet<>(List.of(
                                // user-service - chỉ đọc
                                requirePermission.apply("user-service:departments:read"),
                                requirePermission.apply("user-service:employees:read"),
                                requirePermission.apply("user-service:positions:read"),
                                requirePermission.apply("user-service:users:read"),
                                // job-service - chỉ đọc
                                requirePermission.apply("job-service:job-positions:read"),
                                requirePermission.apply("job-service:recruitment-requests:read"),
                                requirePermission.apply("job-service:job-skills:read"),
                                requirePermission.apply("job-service:job-categories:read"),
                                // candidate-service - đọc và manage candidates của mình (applications merged
                                // into candidates)
                                requirePermission.apply("candidate-service:candidates:read"),
                                requirePermission.apply("candidate-service:comments:read"),
                                requirePermission.apply("candidate-service:comments:manage"),
                                // communications-service - chỉ đọc
                                requirePermission.apply("communications-service:schedules:read"),
                                requirePermission.apply("communications-service:notifications:read"),
                                // workflow-service - chỉ đọc
                                requirePermission.apply("workflow-service:workflows:read"),
                                requirePermission.apply("workflow-service:approval-trackings:read"),
                                // notification-service - chỉ đọc
                                requirePermission.apply("notification-service:notifications:read"),
                                // email-service - chỉ đọc
                                requirePermission.apply("email-service:emails:read"))));

                roleRepository.saveAll(Arrays.asList(adminRole, ceoRole, managerRole, staffRole));
                logger.info("Đã gán quyền cho các vai trò.");

                // =====================================================================================
                // 3. TẠO NHÂN VIÊN (EMPLOYEES) - TẠO TRƯỚC
                // =====================================================================================
                Employee ceoEmployee = buildEmployee("Nguyễn Văn An", "an.nguyen@company.com", "0901234567",
                                directorBoard, ceoPosition, true);

                Employee adminEmployee = buildEmployee("Admin", "admin@gmail.com", "0987654321", null, adminPosition,
                                true);

                // Nhân sự
                Employee hrManagerEmployee = buildEmployee("Trần Thị Bích", "bich.tran@company.com", "0912345678",
                                hrDepartment, managerPosition, true);

                Employee hrStaff1Employee = buildEmployee("Lê Văn Cường", "cuong.le@company.com", "0918765432",
                                hrDepartment, staffPosition, true);

                // Kinh doanh
                Employee salesManagerEmployee = buildEmployee("Võ Minh Long", "long.vo@company.com", "0933445566",
                                salesDepartment, managerPosition, true);

                // Kỹ thuật
                Employee salesStaffEmployee = buildEmployee("Phan Thị Hồng", "hong.phan@company.com", "0933111222",
                                salesDepartment, staffPosition, true);

                // Kỹ thuật
                Employee techManagerEmployee = buildEmployee("Bùi Đức Huy", "huy.bui@company.com", "0945678901",
                                techDepartment, managerPosition, true);

                Employee techStaffEmployee = buildEmployee("Ngô Đình Phúc", "phuc.ngo@company.com", "0945123123",
                                techDepartment, staffPosition, true);

                // Marketing
                Employee marketingManagerEmployee = buildEmployee("Đặng Minh Thư", "thu.dang@company.com",
                                "0956234567", marketingDepartment, managerPosition, true);

                Employee marketingStaffEmployee = buildEmployee("Hoàng Thu Hà", "ha.hoang@company.com", "0956345678",
                                marketingDepartment, staffPosition, true);

                // Kế toán
                Employee accountingManagerEmployee = buildEmployee("Vũ Hồng Sơn", "son.vu@company.com",
                                "0967456789", accountingDepartment, managerPosition, true);

                Employee accountingStaffEmployee = buildEmployee("Đỗ Ngọc Ánh", "anh.do@company.com", "0967567890",
                                accountingDepartment, staffPosition, true);

                List<Employee> employees = Arrays.asList(
                                ceoEmployee,
                                adminEmployee,
                                hrManagerEmployee,
                                hrStaff1Employee,
                                salesManagerEmployee,
                                salesStaffEmployee,
                                techManagerEmployee,
                                techStaffEmployee,
                                marketingManagerEmployee,
                                marketingStaffEmployee,
                                accountingManagerEmployee,
                                accountingStaffEmployee);

                employeeRepository.saveAll(employees);
                logger.info("Đã tạo " + employeeRepository.count() + " nhân viên.");

                // Refresh employees từ database để tránh detached entity
                ceoEmployee = employeeRepository.findById(ceoEmployee.getId()).orElse(ceoEmployee);
                adminEmployee = employeeRepository.findById(adminEmployee.getId()).orElse(adminEmployee);
                hrManagerEmployee = employeeRepository.findById(hrManagerEmployee.getId()).orElse(hrManagerEmployee);
                hrStaff1Employee = employeeRepository.findById(hrStaff1Employee.getId()).orElse(hrStaff1Employee);
                salesManagerEmployee = employeeRepository.findById(salesManagerEmployee.getId())
                                .orElse(salesManagerEmployee);
                salesStaffEmployee = employeeRepository.findById(salesStaffEmployee.getId())
                                .orElse(salesStaffEmployee);
                techManagerEmployee = employeeRepository.findById(techManagerEmployee.getId())
                                .orElse(techManagerEmployee);
                techStaffEmployee = employeeRepository.findById(techStaffEmployee.getId())
                                .orElse(techStaffEmployee);
                marketingManagerEmployee = employeeRepository.findById(marketingManagerEmployee.getId())
                                .orElse(marketingManagerEmployee);
                marketingStaffEmployee = employeeRepository.findById(marketingStaffEmployee.getId())
                                .orElse(marketingStaffEmployee);
                accountingManagerEmployee = employeeRepository.findById(accountingManagerEmployee.getId())
                                .orElse(accountingManagerEmployee);
                accountingStaffEmployee = employeeRepository.findById(accountingStaffEmployee.getId())
                                .orElse(accountingStaffEmployee);

                // =====================================================================================
                // 4. TẠO NGƯỜI DÙNG (USERS) - TẠO SAU VÀ LINK VỚI EMPLOYEE ĐÃ TỒN TẠI
                // =====================================================================================
                User ceo = buildUser("Nguyễn Văn An", "an.nguyen@company.com", ceoRole, ceoEmployee, true);
                User admin = buildUser("Admin", "admin@gmail.com", adminRole, adminEmployee, true);
                User hrManager = buildUser("Trần Thị Bích", "bich.tran@company.com", managerRole, hrManagerEmployee,
                                true);
                User hrStaff1 = buildUser("Lê Văn Cường", "cuong.le@company.com", staffRole, hrStaff1Employee, true);
                User salesManager = buildUser("Võ Minh Long", "long.vo@company.com", managerRole, salesManagerEmployee,
                                true);
                User salesStaff = buildUser("Phan Thị Hồng", "hong.phan@company.com", staffRole,
                                salesStaffEmployee, true);
                User techManager = buildUser("Bùi Đức Huy", "huy.bui@company.com", managerRole, techManagerEmployee,
                                true);
                User techStaff = buildUser("Ngô Đình Phúc", "phuc.ngo@company.com", staffRole, techStaffEmployee,
                                true);
                User marketingManager = buildUser("Đặng Minh Thư", "thu.dang@company.com", managerRole,
                                marketingManagerEmployee, true);
                User marketingStaff = buildUser("Hoàng Thu Hà", "ha.hoang@company.com", staffRole,
                                marketingStaffEmployee, true);
                User accountingManager = buildUser("Vũ Hồng Sơn", "son.vu@company.com", managerRole,
                                accountingManagerEmployee, true);
                User accountingStaff = buildUser("Đỗ Ngọc Ánh", "anh.do@company.com", staffRole,
                                accountingStaffEmployee, true);

                List<User> users = Arrays.asList(
                                ceo,
                                admin,
                                hrManager,
                                hrStaff1,
                                salesManager,
                                salesStaff,
                                techManager,
                                techStaff,
                                marketingManager,
                                marketingStaff,
                                accountingManager,
                                accountingStaff);

                userRepository.saveAll(users);
                logger.info("Đã tạo " + userRepository.count() + " người dùng.");
                logger.info("Hoàn tất quá trình khởi tạo dữ liệu.");
        }

        private Employee buildEmployee(String name, String email, String phone, Department department,
                        Position position, boolean active) {
                Employee employee = new Employee();
                employee.setName(name);
                employee.setEmail(email);
                employee.setPhone(phone);
                employee.setDepartment(department);
                employee.setPosition(position);
                employee.setStatus(active ? "ACTIVE" : "INACTIVE");
                return employee;
        }

        private User buildUser(String name, String email, Role role, Employee employee, boolean active) {
                User user = new User();
                // User không có field name, name lấy từ Employee
                user.setEmail(email);
                user.setPassword(BCRYPTED_PASSWORD);
                user.setRole(role);
                user.set_active(active);

                // Link với Employee đã tồn tại
                user.setEmployee(employee);
                employee.setUser(user);

                return user;
        }
}
