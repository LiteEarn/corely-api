package br.com.corely.dev.seed;

import br.com.corely.attendance.Attendance;
import br.com.corely.attendance.AttendanceRepository;
import br.com.corely.attendance.AttendanceService;
import br.com.corely.attendance.AttendanceStatus;
import br.com.corely.attendance.dto.AttendanceRequest;
import br.com.corely.classgroup.ClassGroup;
import br.com.corely.classgroup.ClassGroupRepository;
import br.com.corely.classgroup.ClassGroupService;
import br.com.corely.classgroup.dto.ClassGroupRequest;
import br.com.corely.classsession.ClassSession;
import br.com.corely.classsession.ClassSessionRepository;
import br.com.corely.classsession.ClassSessionService;
import br.com.corely.classsession.ClassSessionStatus;
import br.com.corely.finance.membershipplan.MembershipPlan;
import br.com.corely.enrollment.Enrollment;
import br.com.corely.enrollment.EnrollmentRepository;
import br.com.corely.enrollment.EnrollmentService;
import br.com.corely.enrollment.dto.EnrollmentRequest;
import br.com.corely.evaluation.EvaluationService;
import br.com.corely.evaluation.dto.EvaluationRequest;
import br.com.corely.evolution.EvolutionService;
import br.com.corely.evolution.dto.EvolutionRequest;
import br.com.corely.finance.membershipplan.MembershipPlan;
import br.com.corely.finance.membershipplan.MembershipPlanRepository;
import br.com.corely.instructor.Instructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import br.com.corely.instructor.InstructorRepository;
import br.com.corely.instructor.InstructorService;
import br.com.corely.instructor.dto.InstructorRequest;
import br.com.corely.makeup.MakeupRequest;
import br.com.corely.makeup.MakeupRequestRepository;
import br.com.corely.makeup.MakeupRequestService;
import br.com.corely.makeup.MakeupRequestStatus;
import br.com.corely.makeup.dto.MakeupApproveRequest;
import br.com.corely.makeup.dto.MakeupRejectRequest;
import br.com.corely.makeup.dto.MakeupRequestRequest;
import br.com.corely.objective.ObjectiveRepository;
import br.com.corely.objective.ObjectiveService;
import br.com.corely.objective.ObjectiveStatus;
import br.com.corely.objective.dto.ObjectiveRequest;
import br.com.corely.student.Student;
import br.com.corely.student.StudentRepository;
import br.com.corely.student.StudentService;
import br.com.corely.student.dto.StudentRequest;
import br.com.corely.studio.Studio;
import br.com.corely.studio.StudioRepository;
import br.com.corely.user.User;
import br.com.corely.user.UserRepository;
import br.com.corely.user.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeedService {

    private final StudioRepository studioRepository;
    private final UserRepository userRepository;
    private final InstructorRepository instructorRepository;
    private final ClassGroupRepository classGroupRepository;
    private final ClassSessionRepository classSessionRepository;
    private final StudentRepository studentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final AttendanceRepository attendanceRepository;
    private final MakeupRequestRepository makeupRequestRepository;
    private final ObjectiveRepository objectiveRepository;
    private final br.com.corely.evaluation.EvaluationRepository evaluationRepository;
    private final br.com.corely.evolution.EvolutionRepository evolutionRepository;

    private final InstructorService instructorService;
    private final ClassGroupService classGroupService;
    private final ClassSessionService classSessionService;
    private final StudentService studentService;
    private final EnrollmentService enrollmentService;
    private final AttendanceService attendanceService;
    private final MakeupRequestService makeupRequestService;
    private final ObjectiveService objectiveService;
    private final EvaluationService evaluationService;
    private final EvolutionService evolutionService;

    private final PasswordEncoder passwordEncoder;

    private final MembershipPlanRepository membershipPlanRepository;

    private Studio studio;
    private MembershipPlan defaultPlan;
    private List<Instructor> instructors;
    private List<ClassGroup> classGroups;
    private List<UUID> studentIds;
    private final List<String> objectiveTitles = List.of(
            "Emagrecimento", "Hipertrofia", "Mobilidade", "Reabilitacao",
            "Condicionamento", "Postura", "Gestantes", "Idosos"
    );

    public SeedResponse execute() {
        SeedResponse response = new SeedResponse();
        clearAll();

        createStudio();
        createDefaultPlan();
        createUsers();
        authenticateAsAdmin();
        createInstructors();
        createClassGroups();
        createStudents();
        createEnrollments();

        generatePastSessionsAndAttendances();
        startSessions();
        registerAttendances();
        completeSessions();
        cancelSessions();

        createMakeupRequests();
        createEvaluations();
        createEvolutions();

        response.setStudents(studentIds.size());
        response.setClassGroups(classGroups.size());
        response.setSessions((int) classSessionRepository.count());
        response.setAttendances((int) attendanceRepository.count());
        response.setMakeupRequests((int) makeupRequestRepository.count());
        return response;
    }

    @Transactional
    public void clearAll() {
        makeupRequestRepository.deleteAll();
        attendanceRepository.deleteAll();
        classSessionRepository.deleteAll();
        enrollmentRepository.deleteAll();
        evolutionRepository.deleteAll();
        evaluationRepository.deleteAll();
        objectiveRepository.deleteAll();
        classGroupRepository.deleteAll();
        studentRepository.deleteAll();
        instructorRepository.deleteAll();
        userRepository.deleteAll();
        membershipPlanRepository.deleteAll();
        studioRepository.deleteAll();
        studio = null;
        defaultPlan = null;
        instructors = null;
        classGroups = null;
        studentIds = null;
    }

    private void createStudio() {
        studio = new Studio();
        studio.setName("Corely Pilates Studio");
        studio.setActive(true);
        studio = studioRepository.save(studio);
    }

    private void createDefaultPlan() {
        var plan = new MembershipPlan();
        plan.setStudio(studio);
        plan.setName("Plano Básico");
        plan.setDescription("Plano básico de Pilates");
        plan.setMonthlyPrice(BigDecimal.valueOf(199));
        plan.setSessionsPerWeek(2);
        plan.setActive(true);
        defaultPlan = membershipPlanRepository.save(plan);
    }

    private void authenticateAsAdmin() {
        var admin = userRepository.findByEmail("admin@corely.com")
                .orElse(null);
        if (admin != null) {
            var auth = new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
    }

    private void createUsers() {
        String defaultPass = passwordEncoder.encode("123456");

        User admin = new User();
        admin.setName("Administrador");
        admin.setEmail("admin@corely.com");
        admin.setPassword(defaultPass);
        admin.setRole(UserRole.ADMIN);
        admin.setActive(true);
        admin.setStudio(studio);
        userRepository.save(admin);

        User receptionist = new User();
        receptionist.setName("Recepcionista");
        receptionist.setEmail("recepcao@corely.com");
        receptionist.setPassword(defaultPass);
        receptionist.setRole(UserRole.RECEPTIONIST);
        receptionist.setActive(true);
        receptionist.setStudio(studio);
        userRepository.save(receptionist);

        User inst1 = new User();
        inst1.setName("Instrutor 1");
        inst1.setEmail("instrutor1@corely.com");
        inst1.setPassword(defaultPass);
        inst1.setRole(UserRole.INSTRUCTOR);
        inst1.setActive(true);
        inst1.setStudio(studio);
        userRepository.save(inst1);

        User inst2 = new User();
        inst2.setName("Instrutor 2");
        inst2.setEmail("instrutor2@corely.com");
        inst2.setPassword(defaultPass);
        inst2.setRole(UserRole.INSTRUCTOR);
        inst2.setActive(true);
        inst2.setStudio(studio);
        userRepository.save(inst2);
    }

    private void createInstructors() {
        instructors = new ArrayList<>();
        instructors.add(createInstructor("Carlos Eduardo", "carlos.eduardo@corely.com", "11987654321", "Pilates Classico"));
        instructors.add(createInstructor("Fernanda Lima", "fernanda.lima@corely.com", "11987654322", "Pilates Funcional"));
        instructors.add(createInstructor("Joao Pedro", "joao.pedro@corely.com", "11987654323", "Reabilitacao"));
        instructors.add(createInstructor("Mariana Alves", "mariana.alves@corely.com", "11987654324", "Pilates Solo"));
        instructors.add(createInstructor("Ricardo Souza", "ricardo.souza@corely.com", "11987654325", "Power Pilates"));
    }

    private Instructor createInstructor(String name, String email, String phone, String specialty) {
        InstructorRequest req = new InstructorRequest();
        req.setStudioId(studio.getId());
        req.setFullName(name);
        req.setEmail(email);
        req.setPhone(phone);
        req.setSpecialty(specialty);
        var response = instructorService.create(req);
        return instructorRepository.findById(response.getId()).orElseThrow();
    }

    private void createClassGroups() {
        classGroups = new ArrayList<>();

        classGroups.add(createClassGroup("Pilates Iniciante", "Turma para iniciantes em Pilates", instructors.get(0),
                LocalTime.of(7, 0), LocalTime.of(8, 0), 8, true, true, false, true, false, false));
        classGroups.add(createClassGroup("Pilates Avancado", "Turma para alunos avancados", instructors.get(1),
                LocalTime.of(8, 0), LocalTime.of(9, 0), 6, false, true, true, false, true, false));
        classGroups.add(createClassGroup("Pilates Funcional", "Pilates com enfoque funcional", instructors.get(2),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 10, true, false, true, false, true, false));
        classGroups.add(createClassGroup("Pilates Solo", "Pilates solo com aparelhos portateis", instructors.get(3),
                LocalTime.of(10, 0), LocalTime.of(11, 0), 8, false, true, false, true, false, true));
        classGroups.add(createClassGroup("Gestantes", "Pilates para gestantes", instructors.get(4),
                LocalTime.of(14, 0), LocalTime.of(15, 0), 6, true, false, true, false, true, false));
        classGroups.add(createClassGroup("Idosos", "Pilates para a melhor idade", instructors.get(0),
                LocalTime.of(15, 0), LocalTime.of(16, 0), 8, true, true, true, true, true, false));
        classGroups.add(createClassGroup("Reabilitacao", "Pilates para reabilitacao", instructors.get(1),
                LocalTime.of(17, 0), LocalTime.of(18, 0), 6, true, false, true, false, true, false));
        classGroups.add(createClassGroup("Mobilidade", "Exercicios para mobilidade articular", instructors.get(2),
                LocalTime.of(18, 0), LocalTime.of(19, 0), 10, false, true, false, true, false, false));
        classGroups.add(createClassGroup("Power Pilates", "Pilates intenso", instructors.get(3),
                LocalTime.of(7, 0), LocalTime.of(8, 0), 8, true, true, false, false, true, true));
        classGroups.add(createClassGroup("Alongamento", "Aula de alongamento e flexibilidade", instructors.get(4),
                LocalTime.of(8, 0), LocalTime.of(9, 0), 10, true, true, true, true, true, false));
        classGroups.add(createClassGroup("Pilates Intermediario", "Nivel intermediario de Pilates", instructors.get(0),
                LocalTime.of(14, 0), LocalTime.of(15, 0), 6, false, false, true, true, false, true));
        classGroups.add(createClassGroup("Pilates Manha", "Pilates matinal", instructors.get(1),
                LocalTime.of(7, 0), LocalTime.of(8, 0), 4, true, false, false, false, true, false));
        classGroups.add(createClassGroup("Pilates Tarde", "Pilates vespertino", instructors.get(2),
                LocalTime.of(17, 0), LocalTime.of(18, 0), 8, true, true, true, true, true, false));
        classGroups.add(createClassGroup("Pilates Noite", "Pilates noturno", instructors.get(3),
                LocalTime.of(19, 0), LocalTime.of(20, 0), 6, true, true, false, true, false, false));
        classGroups.add(createClassGroup("Pilates Sabado", "Pilates especial de sabado", instructors.get(4),
                LocalTime.of(9, 0), LocalTime.of(10, 0), 10, false, false, false, false, false, true));
    }

    private ClassGroup createClassGroup(String name, String description, Instructor instructor,
                                         LocalTime start, LocalTime end, int capacity,
                                         boolean mon, boolean tue, boolean wed, boolean thu, boolean fri, boolean sat) {
        ClassGroupRequest req = new ClassGroupRequest();
        req.setStudioId(studio.getId());
        req.setInstructorId(instructor.getId());
        req.setName(name);
        req.setDescription(description);
        req.setStartTime(start);
        req.setEndTime(end);
        req.setCapacity(capacity);
        req.setMonday(mon);
        req.setTuesday(tue);
        req.setWednesday(wed);
        req.setThursday(thu);
        req.setFriday(fri);
        req.setSaturday(sat);
        req.setActive(true);
        var response = classGroupService.create(req);
        return classGroupRepository.findById(response.getId()).orElseThrow();
    }

    private void createStudents() {
        studentIds = new ArrayList<>();
        List<StudentInfo> studentInfos = buildStudentInfos();
        for (StudentInfo info : studentInfos) {
            StudentRequest req = new StudentRequest();
            req.setStudioId(studio.getId());
            req.setFullName(info.name);
            req.setPhone(info.phone);
            req.setEmail(info.email);
            req.setBirthDate(info.birthDate);
            req.setActive(true);
            req.setMembershipPlanId(defaultPlan.getId());
            var response = studentService.create(req);
            Student student = studentRepository.findById(response.getId()).orElseThrow();
            assignObjectives(student);
            studentIds.add(student.getId());
        }
    }

    private void assignObjectives(Student student) {
        Random rnd = new Random(student.getId().hashCode());
        int count = 1 + rnd.nextInt(3);
        List<String> shuffled = new ArrayList<>(objectiveTitles);
        Collections.shuffle(shuffled, rnd);
        for (int i = 0; i < count && i < shuffled.size(); i++) {
            ObjectiveRequest req = new ObjectiveRequest();
            req.setStudioId(studio.getId());
            req.setStudentId(student.getId());
            req.setTitle(shuffled.get(i));
            req.setDescription("Objetivo de " + shuffled.get(i).toLowerCase());
            req.setStatus(ObjectiveStatus.ACTIVE);
            req.setStartDate(LocalDate.now().minusMonths(1 + rnd.nextInt(6)));
            if (rnd.nextBoolean()) {
                req.setTargetDate(req.getStartDate().plusMonths(3 + rnd.nextInt(6)));
            }
            objectiveService.create(req);
        }
    }

    private List<StudentInfo> buildStudentInfos() {
        List<StudentInfo> infos = new ArrayList<>();
        Random rnd = new Random(42);

        String[] firstNamesMale = {
                "Joao", "Pedro", "Lucas", "Gabriel", "Marcos", "Felipe", "Rafael", "Daniel", "Bruno", "Diego",
                "Thiago", "Rodrigo", "Eduardo", "Gustavo", "Vinicius", "Leonardo", "Andre", "Carlos", "Paulo", "Sergio",
                "Julio", "Marcelo", "Alexandre", "Ricardo", "Fabio", "Luis", "Fernando", "Roberto", "Renato", "Adriano",
                "Caio", "Igor", "Vitor", "Hugo", "Luciano", "Tiago", "Patrick", "William", "Samuel", "Jose"
        };

        String[] firstNamesFemale = {
                "Ana", "Maria", "Juliana", "Fernanda", "Patricia", "Aline", "Camila", "Larissa", "Beatriz", "Marina",
                "Carla", "Luciana", "Renata", "Vanessa", "Amanda", "Roberta", "Tatiane", "Debora", "Priscila", "Jessica",
                "Bruna", "Marcela", "Leticia", "Viviane", "Raquel", "Isabela", "Caroline", "Daniela", "Elaine", "Simone",
                "Adriana", "Cristiane", "Fabiana", "Monica", "Sabrina", "Lucia", "Rita", "Helena", "Sandra", "Flavia"
        };

        String[] lastNames = {
                "Silva", "Santos", "Oliveira", "Souza", "Rodrigues", "Ferreira", "Alves", "Pereira", "Lima", "Gomes",
                "Costa", "Ribeiro", "Martins", "Carvalho", "Almeida", "Lopes", "Soares", "Fernandes", "Vieira", "Barbosa",
                "Rocha", "Dias", "Nascimento", "Andrade", "Moreira", "Mendes", "Araujo", "Cardoso", "Teixeira", "Cavalcanti",
                "Melo", "Barros", "Castro", "Campos", "Correia", "Freitas", "Duarte", "Borges", "Nunes", "Goncalves"
        };

        int totalStudents = 65 + rnd.nextInt(16);
        for (int i = 0; i < totalStudents; i++) {
            boolean isMale = rnd.nextBoolean();
            String[] firstNames = isMale ? firstNamesMale : firstNamesFemale;
            String firstName = firstNames[rnd.nextInt(firstNames.length)];
            String lastName = lastNames[rnd.nextInt(lastNames.length)];
            String middleName = rnd.nextBoolean() ? " " + lastNames[rnd.nextInt(lastNames.length)] : "";
            String fullName = firstName + middleName + " " + lastName;

            int age = 18 + rnd.nextInt(58);
            LocalDate birthDate = LocalDate.now().minusYears(age).minusDays(rnd.nextInt(365));
            String email = firstName.toLowerCase() + "." + lastName.toLowerCase() + i + "@email.com";
            String phone = "(11) 9" + (900000000 + rnd.nextInt(99999999));

            infos.add(new StudentInfo(fullName, phone, email, birthDate));
        }
        return infos;
    }

    private record StudentInfo(String name, String phone, String email, LocalDate birthDate) {}

    private void createEnrollments() {
        if (studentIds.isEmpty() || classGroups.isEmpty()) return;
        Random rnd = new Random(123);
        double[] occupancyLevels = {0.2, 0.4, 0.6, 0.8, 1.0};

        for (int cgIdx = 0; cgIdx < classGroups.size(); cgIdx++) {
            ClassGroup cg = classGroups.get(cgIdx);
            double occupancy = occupancyLevels[cgIdx % occupancyLevels.length];
            int targetEnrollments = Math.min((int) Math.ceil(cg.getCapacity() * occupancy), cg.getCapacity());
            if (targetEnrollments == 0) continue;

            List<UUID> shuffled = new ArrayList<>(studentIds);
            Collections.shuffle(shuffled, rnd);
            int enrolled = 0;
            for (UUID studentId : shuffled) {
                if (enrolled >= targetEnrollments) break;
                try {
                    EnrollmentRequest req = new EnrollmentRequest();
                    req.setStudioId(studio.getId());
                    req.setStudentId(studentId);
                    req.setClassGroupId(cg.getId());
                    req.setEnrollmentDate(LocalDate.now().minusDays(rnd.nextInt(30)));
                    req.setActive(true);
                    enrollmentService.create(req);
                    enrolled++;
                } catch (Exception ignored) {}
            }
        }
    }

    private void generatePastSessionsAndAttendances() {
        Random rnd = new Random(456);
        LocalDate today = LocalDate.now();

        for (ClassGroup cg : classGroups) {
            Set<DayOfWeek> activeDays = new HashSet<>();
            if (Boolean.TRUE.equals(cg.getMonday())) activeDays.add(DayOfWeek.MONDAY);
            if (Boolean.TRUE.equals(cg.getTuesday())) activeDays.add(DayOfWeek.TUESDAY);
            if (Boolean.TRUE.equals(cg.getWednesday())) activeDays.add(DayOfWeek.WEDNESDAY);
            if (Boolean.TRUE.equals(cg.getThursday())) activeDays.add(DayOfWeek.THURSDAY);
            if (Boolean.TRUE.equals(cg.getFriday())) activeDays.add(DayOfWeek.FRIDAY);
            if (Boolean.TRUE.equals(cg.getSaturday())) activeDays.add(DayOfWeek.SATURDAY);
            if (Boolean.TRUE.equals(cg.getSunday())) activeDays.add(DayOfWeek.SUNDAY);
            if (activeDays.isEmpty()) continue;

            for (int dayOffset = -30; dayOffset < 0; dayOffset++) {
                LocalDate date = today.plusDays(dayOffset);
                if (!activeDays.contains(date.getDayOfWeek())) continue;
                if (classSessionRepository.existsByClassGroupIdAndSessionDate(cg.getId(), date)) continue;

                ClassSession session = new ClassSession();
                session.setClassGroup(cg);
                session.setInstructor(cg.getInstructor());
                session.setSessionDate(date);
                session.setStartTime(cg.getStartTime());
                session.setEndTime(cg.getEndTime());
                session.setStatus(ClassSessionStatus.SCHEDULED);
                session = classSessionRepository.save(session);

                try {
                    classSessionService.start(session.getId());
                } catch (Exception e) {
                    continue;
                }

                List<Enrollment> enrollments = enrollmentRepository.findByClassGroupIdAndActiveTrue(cg.getId());
                for (Enrollment enrollment : enrollments) {
                    try {
                        AttendanceRequest attReq = new AttendanceRequest();
                        attReq.setEnrollmentId(enrollment.getId());
                        double dice = rnd.nextDouble();
                        attReq.setStatus(dice < 0.7 ? AttendanceStatus.PRESENT
                                : dice < 0.85 ? AttendanceStatus.ABSENT : AttendanceStatus.JUSTIFIED);
                        attendanceService.register(session.getId(), attReq);
                    } catch (Exception ignored) {}
                }

                try {
                    classSessionService.complete(session.getId());
                } catch (Exception ignored) {}
            }
        }
    }

    private void startSessions() {
        List<ClassSession> todaySessions = classSessionRepository.findAll().stream()
                .filter(s -> s.getSessionDate().equals(LocalDate.now())
                        && s.getStatus() == ClassSessionStatus.SCHEDULED)
                .toList();
        for (int i = 0; i < todaySessions.size() * 0.7; i++) {
            try {
                classSessionService.start(todaySessions.get(i).getId());
            } catch (Exception ignored) {}
        }
    }

    private int registerAttendances() {
        List<ClassSession> inProgress = classSessionRepository.findByStatus(ClassSessionStatus.IN_PROGRESS);
        if (inProgress.isEmpty()) return 0;

        int count = 0;
        int target = 250;
        Random rnd = new Random(789);

        for (ClassSession session : inProgress) {
            if (count >= target) break;
            List<Enrollment> enrollments = enrollmentRepository.findByClassGroupIdAndActiveTrue(session.getClassGroup().getId());
            if (enrollments.isEmpty()) continue;

            int remaining = target - count;
            int perSession = Math.min(enrollments.size(), Math.max(1, remaining / Math.max(1, inProgress.size() - inProgress.indexOf(session))));

            for (int i = 0; i < perSession && i < enrollments.size() && count < target; i++) {
                try {
                    AttendanceRequest attReq = new AttendanceRequest();
                    attReq.setEnrollmentId(enrollments.get(i).getId());
                    double dice = rnd.nextDouble();
                    attReq.setStatus(dice < 0.65 ? AttendanceStatus.PRESENT
                            : dice < 0.85 ? AttendanceStatus.ABSENT : AttendanceStatus.JUSTIFIED);
                    attendanceService.register(session.getId(), attReq);
                    count++;
                } catch (Exception ignored) {}
            }
        }
        return count;
    }

    private void completeSessions() {
        List<ClassSession> inProgress = classSessionRepository.findByStatus(ClassSessionStatus.IN_PROGRESS);
        int toComplete = (int) (inProgress.size() * 0.4);
        for (int i = 0; i < toComplete && i < inProgress.size(); i++) {
            try {
                classSessionService.complete(inProgress.get(i).getId());
            } catch (Exception ignored) {}
        }
    }

    private void cancelSessions() {
        List<ClassSession> futureSessions = classSessionRepository.findAll().stream()
                .filter(s -> s.getSessionDate().isAfter(LocalDate.now())
                        && s.getStatus() == ClassSessionStatus.SCHEDULED)
                .toList();
        int toCancel = (int) (futureSessions.size() * 0.1);
        for (int i = 0; i < toCancel && i < futureSessions.size(); i++) {
            try {
                classSessionService.cancel(futureSessions.get(i).getId(), null, null);
            } catch (Exception ignored) {}
        }
    }

    private void createMakeupRequests() {
        List<Attendance> absent = attendanceRepository.findAll().stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT)
                .toList();
        if (absent.isEmpty()) return;
        int target = Math.min(40, absent.size());
        Random rnd = new Random(111);
        List<Attendance> shuffled = new ArrayList<>(absent);
        Collections.shuffle(shuffled, rnd);

        for (int i = 0; i < target; i++) {
            try {
                MakeupRequestRequest req = new MakeupRequestRequest();
                req.setReason("Imprevisto pessoal - " + (i + 1));
                makeupRequestService.request(shuffled.get(i).getId(), req);
            } catch (Exception ignored) {}
        }

        List<MakeupRequest> requested = makeupRequestRepository.findAll().stream()
                .filter(m -> m.getStatus() == MakeupRequestStatus.REQUESTED)
                .toList();
        for (int i = 0; i < requested.size(); i++) {
            try {
                UUID mrId = requested.get(i).getId();
                UUID attendanceId = requested.get(i).getAttendance().getId();
                if (i < requested.size() * 0.1) {
                    Attendance att = attendanceRepository.findById(attendanceId).orElse(null);
                    if (att != null) {
                        List<ClassSession> future = classSessionRepository
                                .findByClassGroupIdAndSessionDateGreaterThanEqualAndStatus(
                                        att.getEnrollment().getClassGroup().getId(),
                                        LocalDate.now(), ClassSessionStatus.SCHEDULED);
                        if (!future.isEmpty()) {
                            MakeupApproveRequest appReq = new MakeupApproveRequest();
                            appReq.setTargetSessionId(future.get(0).getId());
                            makeupRequestService.approve(mrId, appReq);
                        }
                    }
                } else if (i < requested.size() * 0.2) {
                    MakeupRejectRequest rejReq = new MakeupRejectRequest();
                    rejReq.setReason("Horario indisponivel");
                    makeupRequestService.reject(mrId, rejReq);
                } else if (i < requested.size() * 0.3) {
                    MakeupRequest mr = makeupRequestRepository.findById(mrId).orElse(null);
                    if (mr != null) {
                        mr.setStatus(MakeupRequestStatus.USED);
                        makeupRequestRepository.save(mr);
                    }
                } else if (i < requested.size() * 0.35) {
                    MakeupRequest mr = makeupRequestRepository.findById(mrId).orElse(null);
                    if (mr != null) {
                        mr.setStatus(MakeupRequestStatus.EXPIRED);
                        mr.setRejectionReason("Prazo de validade expirado");
                        makeupRequestRepository.save(mr);
                    }
                }
            } catch (Exception ignored) {}
        }
    }

    private void createEvaluations() {
        int target = Math.min(40, studentIds.size());
        Random rnd = new Random(222);
        List<UUID> shuffled = new ArrayList<>(studentIds);
        Collections.shuffle(shuffled, rnd);

        for (int i = 0; i < target; i++) {
            try {
                EvaluationRequest req = new EvaluationRequest();
                req.setStudioId(studio.getId());
                req.setStudentId(shuffled.get(i));
                req.setEvaluationDate(LocalDate.now().minusDays(rnd.nextInt(90)));
                req.setWeight(BigDecimal.valueOf(50 + rnd.nextDouble() * 50).setScale(2, RoundingMode.HALF_UP));
                req.setHeight(BigDecimal.valueOf(1.50 + rnd.nextDouble() * 0.40).setScale(2, RoundingMode.HALF_UP));
                req.setObservations("Avaliacao inicial - tudo dentro dos parametros esperados");
                evaluationService.create(req);
            } catch (Exception ignored) {}
        }
    }

    private void createEvolutions() {
        Random rnd = new Random(333);
        int target = 120 + rnd.nextInt(31);
        List<UUID> shuffled = new ArrayList<>(studentIds);
        Collections.shuffle(shuffled, rnd);

        for (int i = 0; i < target && !shuffled.isEmpty(); i++) {
            UUID studentId = shuffled.get(i % shuffled.size());
            try {
                EvolutionRequest req = new EvolutionRequest();
                req.setStudioId(studio.getId());
                req.setStudentId(studentId);
                req.setEvolutionDate(LocalDate.now().minusDays(rnd.nextInt(180)));
                req.setTitle("Evolucao " + (i + 1));
                req.setDescription("Progresso satisfatorio. Aluno demonstra melhora na execucao dos exercicios.");
                evolutionService.create(req);
            } catch (Exception ignored) {}
        }
    }

    @Transactional
    public void ensureDashboardData() {
        if (defaultPlan == null) {
            defaultPlan = membershipPlanRepository.findFirstByStudioId(studio.getId());
            if (defaultPlan == null) {
                createDefaultPlan();
            }
        }
        LocalDate today = LocalDate.now();
        List<ClassSession> todaySessions = classSessionRepository.findBySessionDate(today);
        if (todaySessions.isEmpty()) {
            List<ClassGroup> activeGroups = classGroupRepository.findByStudioIdAndActiveTrue(studio.getId());
            if (!activeGroups.isEmpty()) {
                classSessionService.generateSessionsForGroup(activeGroups.get(0));
            }
        }
        for (ClassSession s : classSessionRepository.findBySessionDate(today)) {
            if (s.getStatus() == ClassSessionStatus.SCHEDULED) {
                try { classSessionService.start(s.getId()); } catch (Exception ignored) {}
            }
        }
        List<Attendance> absentList = attendanceRepository.findAll().stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT).toList();
        if (absentList.isEmpty()) {
            List<ClassSession> inProg = classSessionRepository.findByStatus(ClassSessionStatus.IN_PROGRESS);
            if (!inProg.isEmpty()) {
                List<Enrollment> ens = enrollmentRepository.findByClassGroupIdAndActiveTrue(inProg.get(0).getClassGroup().getId());
                if (!ens.isEmpty()) {
                    try {
                        AttendanceRequest attReq = new AttendanceRequest();
                        attReq.setEnrollmentId(ens.get(0).getId());
                        attReq.setStatus(AttendanceStatus.ABSENT);
                        attendanceService.register(inProg.get(0).getId(), attReq);
                    } catch (Exception ignored) {}
                }
            }
        }
        List<MakeupRequest> pending = makeupRequestRepository.findAll().stream()
                .filter(m -> m.getStatus() == MakeupRequestStatus.REQUESTED).toList();
        if (pending.isEmpty()) {
            List<Attendance> abs = attendanceRepository.findAll().stream()
                    .filter(a -> a.getStatus() == AttendanceStatus.ABSENT).toList();
            if (!abs.isEmpty()) {
                try {
                    MakeupRequestRequest req = new MakeupRequestRequest();
                    req.setReason("Falta justificada por problemas de saude");
                    makeupRequestService.request(abs.get(0).getId(), req);
                } catch (Exception ignored) {}
            }
        }
    }

    public void seedStudentsOnly() {
        findOrCreateStudio();
        createDefaultPlan();
        if (userRepository.findByEmail("admin@corely.com").isEmpty()) {
            createUsers();
        }
        authenticateAsAdmin();
        createInstructors();
        createClassGroups();
        enrollmentRepository.deleteAll();
        objectiveRepository.deleteAll();
        studentRepository.deleteAll();
        studentIds = new ArrayList<>();
        createStudents();
        createEnrollments();
    }

    public void seedAttendanceOnly() {
        findOrCreateStudio();
        makeupRequestRepository.deleteAll();
        attendanceRepository.deleteAll();
        List<ClassSession> allSessions = classSessionRepository.findAll();
        if (allSessions.isEmpty()) {
            if (instructors == null || instructors.isEmpty()) createInstructors();
            if (classGroups == null || classGroups.isEmpty()) createClassGroups();
        }
        startSessions();
        registerAttendances();
        completeSessions();
    }

    public void seedMakeupOnly() {
        makeupRequestRepository.deleteAll();
        List<Attendance> absent = attendanceRepository.findAll().stream()
                .filter(a -> a.getStatus() == AttendanceStatus.ABSENT).toList();
        if (absent.isEmpty()) {
            seedAttendanceOnly();
        } else {
            createMakeupRequests();
        }
    }

    private void findOrCreateStudio() {
        studio = studioRepository.findFirstByActiveTrueOrderByName().orElse(null);
        if (studio == null) {
            createStudio();
        }
    }
}
