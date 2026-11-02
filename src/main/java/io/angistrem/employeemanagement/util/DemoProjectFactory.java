package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.BudgetItem;
import io.angistrem.employeemanagement.model.ProjectDocument;
import io.angistrem.employeemanagement.model.ProjectResource;
import io.angistrem.employeemanagement.model.SubTask;
import io.angistrem.employeemanagement.model.Task;
import io.angistrem.employeemanagement.model.TaskDependency;
import io.angistrem.employeemanagement.model.TaskResourceAssignment;
import io.angistrem.employeemanagement.model.TaskStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DemoProjectFactory {

    private DemoProjectFactory() {
    }

    public static ProjectDocument createDemoProject() {
        Task.resetCounter();

        LocalDate base = LocalDate.of(2026, 1, 12);

        ProjectResource chiefEngineer = resource("Главный инженер проекта", "PERSON", "Генпроект", "ГИП", 1.0, 28000);
        ProjectResource projectOffice = resource("Проектный офис", "PERSON", "Заказчик", "Координация", 1.0, 18000);
        ProjectResource financeTeam = resource("Финансовый блок", "PERSON", "Заказчик", "Финансы и договоры", 1.0, 22000);
        ProjectResource geodesy = resource("Геодезия", "CONTRACTOR", "ГеоСервис", "Геодезисты", 1.0, 45000);
        ProjectResource monolithCrew = resource("Монолитная бригада", "CONTRACTOR", "МонолитСтрой", "Монолит", 2.0, 160000);
        ProjectResource steelCrew = resource("Монтаж металлокаркаса", "CONTRACTOR", "КаркасПромСтрой", "Каркас", 2.0, 175000);
        ProjectResource electricalCrew = resource("Электромонтажная бригада", "CONTRACTOR", "ИнжСистемы", "Электрика", 1.0, 98000);
        ProjectResource processInstall = resource("Монтаж технологической линии", "CONTRACTOR", "ТехМонтаж", "Техмонтаж", 1.0, 190000);
        ProjectResource commissioning = resource("ПНР команда", "CONTRACTOR", "ПНР Сервис", "ПНР", 1.0, 140000);

        Task g1 = group("Инициация и проектирование");
        Task g2 = group("Контракты и подготовка площадки");
        Task g3 = group("Строительно-монтажные работы");
        Task g4 = group("Технологическая часть и ввод");

        Task t11 = task("Предпроектная подготовка и ТЭО", "Проектный офис", base, base.plusDays(29), TaskStatus.COMPLETED, "Высокий");
        t11.addSubTask(subTask(t11, "Сбор исходных данных", "Проектный офис", base, base.plusDays(8), "компл.", 1, 1));
        t11.addSubTask(subTask(t11, "ТЭО и финансовая модель", "Финансовый блок", base.plusDays(5), base.plusDays(21), "компл.", 1, 1));
        t11.addSubTask(subTask(t11, "Утверждение инвестиционного решения", "Совет проекта", base.plusDays(22), base.plusDays(29), "этап", 1, 1));

        Task t12 = task("Земля и разрешительная документация", "ЮрЗемРесурс", base.plusDays(8), base.plusDays(63), TaskStatus.COMPLETED, "Высокий");
        t12.addSubTask(subTask(t12, "Оформление участка и сервитутов", "ЮрЗемРесурс", base.plusDays(8), base.plusDays(28), "компл.", 1, 1));
        t12.addSubTask(subTask(t12, "ГПЗУ и ТУ", "ЮрЗемРесурс", base.plusDays(18), base.plusDays(44), "компл.", 1, 1));
        t12.addSubTask(subTask(t12, "Разрешение на строительство", "ЮрЗемРесурс", base.plusDays(45), base.plusDays(63), "док.", 1, 1));

        Task t13 = task("Проектирование стадий П и РД", "Генпроект", base.plusDays(24), base.plusDays(128), TaskStatus.IN_PROGRESS, "Критический");
        t13.addSubTask(subTask(t13, "Технологическая концепция и master-plan", "Генпроект", base.plusDays(24), base.plusDays(44), "компл.", 1, 1));
        t13.addSubTask(subTask(t13, "Стадия П", "Генпроект", base.plusDays(38), base.plusDays(82), "компл.", 1, 1));
        t13.addSubTask(subTask(t13, "Экспертиза", "ГосЭкспертиза", base.plusDays(83), base.plusDays(104), "компл.", 1, 1));
        t13.addSubTask(subTask(t13, "Рабочая документация", "Генпроект", base.plusDays(96), base.plusDays(128), "компл.", 1, 0));

        g1.addChildTask(t11);
        g1.addChildTask(t12);
        g1.addChildTask(t13);
        finalizeGroup(g1);

        Task t21 = task("Финансирование и контрактование", "Финансовый блок", base.plusDays(35), base.plusDays(119), TaskStatus.IN_PROGRESS, "Высокий");
        t21.setRequireAdvanceBeforeStart(true);
        t21.setContractAdvanceDate(base.plusDays(52));
        t21.setRequireKsBeforeStart(true);
        t21.setKsPaymentDate(base.plusDays(110));
        t21.addSubTask(subTask(t21, "Лимиты CAPEX и cash-flow", "Финансовый блок", base.plusDays(35), base.plusDays(50), "компл.", 1, 1));
        t21.addSubTask(subTask(t21, "Тендер генподряда", "Закупочный комитет", base.plusDays(48), base.plusDays(76), "компл.", 1, 1));
        t21.addSubTask(subTask(t21, "Контрактование ключевых поставок", "Финансовый блок", base.plusDays(74), base.plusDays(119), "договор", 4, 3));

        Task t22 = task("Подготовка площадки", "СтройГенПодряд", base.plusDays(70), base.plusDays(129), TaskStatus.IN_PROGRESS, "Высокий");
        t22.addSubTask(subTask(t22, "Мобилизация штаба", "СтройГенПодряд", base.plusDays(70), base.plusDays(78), "компл.", 1, 1));
        t22.addSubTask(subTask(t22, "Геодезия и вынос осей", "ГеоСервис", base.plusDays(76), base.plusDays(86), "компл.", 1, 1));
        t22.addSubTask(subTask(t22, "Временные сети и ограждение", "СтройГенПодряд", base.plusDays(82), base.plusDays(103), "компл.", 1, 1));
        t22.addSubTask(subTask(t22, "Планировка территории", "СтройГенПодряд", base.plusDays(98), base.plusDays(129), "га", 14, 11));

        Task t23 = task("Ранняя логистика и площадка поставок", "ПромСнабИнвест", base.plusDays(86), base.plusDays(148), TaskStatus.PLANNED, "Средний");
        t23.addSubTask(subTask(t23, "Схема складирования", "ПромСнабИнвест", base.plusDays(86), base.plusDays(97), "компл.", 1, 0));
        t23.addSubTask(subTask(t23, "Временные склады", "ПромСнабИнвест", base.plusDays(95), base.plusDays(116), "компл.", 1, 0));
        t23.addSubTask(subTask(t23, "Площадка негабаритных поставок", "ПромСнабИнвест", base.plusDays(112), base.plusDays(148), "компл.", 1, 0));

        g2.addChildTask(t21);
        g2.addChildTask(t22);
        g2.addChildTask(t23);
        finalizeGroup(g2);

        Task t31 = task("Фундаменты и подземная часть", "МонолитСтрой", base.plusDays(122), base.plusDays(205), TaskStatus.PLANNED, "Критический");
        t31.addSubTask(subTask(t31, "Котлован и водопонижение", "МонолитСтрой", base.plusDays(122), base.plusDays(146), "м3", 18500, 0));
        t31.addSubTask(subTask(t31, "Свайное поле", "МонолитСтрой", base.plusDays(140), base.plusDays(168), "шт", 420, 0));
        t31.addSubTask(subTask(t31, "Ростверки и фундаментные балки", "МонолитСтрой", base.plusDays(166), base.plusDays(194), "м3", 2600, 0));
        t31.addSubTask(subTask(t31, "Подземные вводы", "ИнжСети", base.plusDays(188), base.plusDays(205), "компл.", 1, 0));

        Task t32 = task("Каркас и оболочка здания", "КаркасПромСтрой", base.plusDays(190), base.plusDays(305), TaskStatus.PLANNED, "Критический");
        t32.addSubTask(subTask(t32, "Колонны и стойки", "КаркасПромСтрой", base.plusDays(190), base.plusDays(221), "т", 870, 0));
        t32.addSubTask(subTask(t32, "Фермы и связи", "КаркасПромСтрой", base.plusDays(214), base.plusDays(246), "т", 760, 0));
        t32.addSubTask(subTask(t32, "Перекрытия", "КаркасПромСтрой", base.plusDays(238), base.plusDays(268), "м2", 12500, 0));
        t32.addSubTask(subTask(t32, "Стеновые панели и кровля", "КаркасПромСтрой", base.plusDays(260), base.plusDays(305), "м2", 21400, 0));

        Task t33 = task("Наружные сети и благоустройство", "ИнжСистемы", base.plusDays(214), base.plusDays(326), TaskStatus.PLANNED, "Высокий");
        t33.addSubTask(subTask(t33, "Электроснабжение и КТП", "ИнжСистемы", base.plusDays(214), base.plusDays(252), "компл.", 1, 0));
        t33.addSubTask(subTask(t33, "Водоснабжение и пожарные резервуары", "ИнжСистемы", base.plusDays(228), base.plusDays(270), "компл.", 1, 0));
        t33.addSubTask(subTask(t33, "Дороги и площадки", "ДорИнфраСтрой", base.plusDays(268), base.plusDays(314), "м2", 9300, 0));
        t33.addSubTask(subTask(t33, "Благоустройство первой очереди", "ДорИнфраСтрой", base.plusDays(300), base.plusDays(326), "компл.", 1, 0));

        Task t34 = task("Внутренние инженерные системы", "ИнжСистемы", base.plusDays(250), base.plusDays(346), TaskStatus.PLANNED, "Высокий");
        t34.addSubTask(subTask(t34, "Силовое оборудование и освещение", "ИнжСистемы", base.plusDays(250), base.plusDays(282), "компл.", 1, 0));
        t34.addSubTask(subTask(t34, "ОВиК производственных помещений", "ИнжСистемы", base.plusDays(260), base.plusDays(310), "компл.", 1, 0));
        t34.addSubTask(subTask(t34, "Слаботочные системы и АПС", "ИнжСистемы", base.plusDays(276), base.plusDays(322), "компл.", 1, 0));
        t34.addSubTask(subTask(t34, "Чистые зоны и финишные покрытия", "ФинишМастер", base.plusDays(314), base.plusDays(346), "м2", 4800, 0));

        g3.addChildTask(t31);
        g3.addChildTask(t32);
        g3.addChildTask(t33);
        g3.addChildTask(t34);
        finalizeGroup(g3);

        Task t41 = task("Длинноцикловые поставки", "ПромСнабИнвест", base.plusDays(104), base.plusDays(308), TaskStatus.IN_PROGRESS, "Высокий");
        t41.addSubTask(subTask(t41, "Заказ технологической линии", "ПромСнабИнвест", base.plusDays(104), base.plusDays(126), "компл.", 1, 1));
        t41.addSubTask(subTask(t41, "Производство оборудования", "ЕвроМехМаш", base.plusDays(127), base.plusDays(236), "компл.", 1, 0));
        t41.addSubTask(subTask(t41, "Логистика и таможня", "ПромСнабИнвест", base.plusDays(220), base.plusDays(278), "компл.", 1, 0));
        t41.addSubTask(subTask(t41, "Приёмка на площадке", "ПромСнабИнвест", base.plusDays(279), base.plusDays(308), "компл.", 1, 0));

        Task t42 = task("Монтаж технологической линии", "ТехМонтаж", base.plusDays(300), base.plusDays(382), TaskStatus.PLANNED, "Критический");
        t42.addSubTask(subTask(t42, "Разгрузка и расконсервация", "ТехМонтаж", base.plusDays(300), base.plusDays(314), "компл.", 1, 0));
        t42.addSubTask(subTask(t42, "Монтаж ядра линии", "ТехМонтаж", base.plusDays(314), base.plusDays(346), "компл.", 1, 0));
        t42.addSubTask(subTask(t42, "Обвязка и подключение", "ТехМонтаж", base.plusDays(340), base.plusDays(370), "компл.", 1, 0));
        t42.addSubTask(subTask(t42, "Интеграция с АСУ ТП", "ТехМонтаж", base.plusDays(362), base.plusDays(382), "компл.", 1, 0));

        Task t43 = task("ПНР, обучение и разрешительный ввод", "ПНР Сервис", base.plusDays(376), base.plusDays(424), TaskStatus.PLANNED, "Критический");
        t43.addSubTask(subTask(t43, "Холодные испытания", "ПНР Сервис", base.plusDays(376), base.plusDays(388), "компл.", 1, 0));
        t43.addSubTask(subTask(t43, "Горячая наладка", "ПНР Сервис", base.plusDays(389), base.plusDays(404), "компл.", 1, 0));
        t43.addSubTask(subTask(t43, "Обучение эксплуатации", "ПНР Сервис", base.plusDays(396), base.plusDays(410), "чел.", 28, 0));
        t43.addSubTask(subTask(t43, "ЗОС и ввод в эксплуатацию", "Заказчик и технадзор", base.plusDays(411), base.plusDays(424), "этап", 1, 0));

        g4.addChildTask(t41);
        g4.addChildTask(t42);
        g4.addChildTask(t43);
        finalizeGroup(g4);

        Task finalMilestone = task("Финальная веха: запуск завода", "Заказчик и технадзор", base.plusDays(424), base.plusDays(424), TaskStatus.PLANNED, "Критический");
        finalMilestone.setTaskType("MILESTONE");
        finalMilestone.setCriticalDependency(true);

        t12.setDependencies(List.of(new TaskDependency(t11.getPosition(), "SS", 5)));
        t13.setDependencies(List.of(
                new TaskDependency(t11.getPosition(), "FS", 0),
                new TaskDependency(t12.getPosition(), "FF", -10)
        ));
        t21.setDependencies(List.of(new TaskDependency(t11.getPosition(), "FS", 0)));
        t22.setDependencies(List.of(
                new TaskDependency(t12.getPosition(), "FS", 0),
                new TaskDependency(t21.getPosition(), "FS", 0)
        ));
        t23.setDependencies(List.of(
                new TaskDependency(t21.getPosition(), "FS", 0),
                new TaskDependency(t22.getPosition(), "SS", 10)
        ));
        t31.setDependencies(List.of(
                new TaskDependency(t13.getPosition(), "FS", 5),
                new TaskDependency(t22.getPosition(), "FS", 0)
        ));
        t32.setDependencies(List.of(new TaskDependency(t31.getPosition(), "FS", 0)));
        t33.setDependencies(List.of(
                new TaskDependency(t31.getPosition(), "FF", -8),
                new TaskDependency(t32.getPosition(), "SS", 18)
        ));
        t34.setDependencies(List.of(
                new TaskDependency(t32.getPosition(), "FS", -10),
                new TaskDependency(t33.getPosition(), "SS", 16)
        ));
        t41.setDependencies(List.of(
                new TaskDependency(t13.getPosition(), "SS", 12),
                new TaskDependency(t21.getPosition(), "FS", 0)
        ));
        t42.setDependencies(List.of(
                new TaskDependency(t41.getPosition(), "FS", 0),
                new TaskDependency(t34.getPosition(), "SS", 10),
                new TaskDependency(t32.getPosition(), "FS", -5)
        ));
        t43.setDependencies(List.of(
                new TaskDependency(t42.getPosition(), "FS", 0),
                new TaskDependency(t34.getPosition(), "FF", 0),
                new TaskDependency(t33.getPosition(), "FF", 0)
        ));
        finalMilestone.setDependencies(List.of(new TaskDependency(t43.getPosition(), "FS", 0)));

        assign(t11, chiefEngineer, "Куратор", 0.6, 120, 118);
        assign(t11, projectOffice, "Координация", 0.8, 160, 154);

        assign(t12, chiefEngineer, "Согласование", 0.4, 88, 84);
        assign(t12, projectOffice, "Документооборот", 0.5, 96, 92);

        assign(t13, chiefEngineer, "ГИП", 0.8, 420, 360);
        assign(t13, projectOffice, "Контроль РД", 0.5, 180, 132);

        assign(t21, financeTeam, "Финансовый контроль", 1.0, 220, 190);
        assign(t21, chiefEngineer, "Техусловия договора", 0.4, 60, 44);

        assign(t22, projectOffice, "Операционный контроль", 0.5, 96, 88);
        assign(t22, geodesy, "Геодезия", 1.0, 72, 70);
        assign(t22, chiefEngineer, "Координация площадки", 0.5, 96, 72);

        assign(t23, projectOffice, "Логистика площадки", 0.4, 60, 44);

        assign(t31, monolithCrew, "Монолит", 1.6, 620, 0);
        assign(t31, chiefEngineer, "Технический надзор", 0.3, 66, 0);

        assign(t32, steelCrew, "Каркас", 1.8, 760, 0);
        assign(t32, chiefEngineer, "Координация каркаса", 0.3, 88, 0);

        assign(t33, electricalCrew, "Наружные сети", 1.0, 340, 0);
        assign(t33, chiefEngineer, "Согласование сетей", 0.4, 92, 0);

        assign(t34, electricalCrew, "Внутренние сети", 1.0, 410, 0);
        assign(t34, chiefEngineer, "Инженерная координация", 0.4, 102, 0);

        assign(t41, projectOffice, "Логистика", 0.4, 84, 60);
        assign(t41, financeTeam, "Платежи поставщикам", 0.3, 54, 38);

        assign(t42, processInstall, "Монтаж линии", 1.0, 460, 0);
        assign(t42, chiefEngineer, "Техническое сопровождение", 0.5, 96, 0);

        assign(t43, commissioning, "ПНР", 1.0, 240, 0);
        assign(t43, chiefEngineer, "Приёмка", 0.4, 72, 0);

        List<Task> topLevel = new ArrayList<>(List.of(g1, g2, g3, g4, finalMilestone));

        List<ProjectResource> resources = List.of(
                chiefEngineer, projectOffice, financeTeam, geodesy, monolithCrew, steelCrew, electricalCrew, processInstall, commissioning
        );

        List<BudgetItem> budgetItems = new ArrayList<>();
        budgetItems.add(budget(t11, "Предпроект и ТЭО", "Проектирование", 4_500_000, 4_250_000, base.plusDays(18), base.plusDays(18), "CLOSED"));
        budgetItems.add(budget(t12, "Разрешительная документация", "Разрешения", 6_800_000, 6_950_000, base.plusDays(40), base.plusDays(44), "CLOSED"));
        budgetItems.add(budget(t13, "Стадия П и РД", "Проектирование", 24_000_000, 19_500_000, base.plusDays(86), null, "IN_PROGRESS"));
        budgetItems.add(budget(t21, "Аванс по генподряду", "Контракты", 35_000_000, 35_000_000, base.plusDays(55), base.plusDays(57), "PAID"));
        budgetItems.add(budget(t21, "Аванс по технологической линии", "Контракты", 48_000_000, 48_000_000, base.plusDays(98), base.plusDays(101), "PAID"));
        budgetItems.add(budget(t22, "Подготовка площадки", "СМР", 18_500_000, 16_200_000, base.plusDays(112), null, "IN_PROGRESS"));
        budgetItems.add(budget(t31, "Фундаменты и подземка", "СМР", 76_000_000, 0, base.plusDays(176), null, "APPROVED"));
        budgetItems.add(budget(t32, "Каркас и оболочка", "СМР", 129_000_000, 0, base.plusDays(250), null, "APPROVED"));
        budgetItems.add(budget(t33, "Наружные сети", "Инженерия", 31_500_000, 0, base.plusDays(286), null, "PLANNED"));
        budgetItems.add(budget(t34, "Внутренние системы", "Инженерия", 58_000_000, 0, base.plusDays(330), null, "PLANNED"));
        budgetItems.add(budget(t41, "Оборудование long-lead", "Оборудование", 152_000_000, 118_000_000, base.plusDays(214), base.plusDays(216), "IN_PROGRESS"));
        budgetItems.add(budget(t42, "Монтаж линии", "Техмонтаж", 46_000_000, 0, base.plusDays(370), null, "PLANNED"));
        budgetItems.add(budget(t43, "ПНР и ввод", "ПНР", 19_500_000, 0, base.plusDays(406), null, "PLANNED"));

        ProjectDocument document = new ProjectDocument();
        document.setProjectName("Демо-проект: строительство и запуск завода металлоконструкций");
        document.setAuthor("Angistrem Demo Team");
        document.setTasks(topLevel);
        document.setResources(resources);
        document.setBudgetItems(budgetItems);
        return document;
    }

    private static Task task(String name, String company, LocalDate start, LocalDate end, String status, String priority) {
        return new Task(name, company, start, end, status, priority);
    }

    private static Task group(String name) {
        Task group = new Task(name, "Штаб проекта", null, null, TaskStatus.IN_PROGRESS, "Высокий");
        group.setTaskType("GROUP");
        return group;
    }

    private static void finalizeGroup(Task group) {
        group.recalculateChildTaskPositions();
        for (Task child : group.getObservableChildTasks()) {
            child.recalculateSubTaskPositions();
        }
        group.updateAggregateFromChildren();
    }

    private static SubTask subTask(Task parent, String name, String company, LocalDate start, LocalDate end,
                                   String unit, Integer volumeProject, Integer volumeActual) {
        return new SubTask(parent, name, company, start, end, unit, volumeProject, volumeActual);
    }

    private static ProjectResource resource(String name, String type, String company, String role, double capacityPerDay, double costPerDay) {
        ProjectResource resource = new ProjectResource();
        resource.setName(name);
        resource.setType(type);
        resource.setCompany(company);
        resource.setRole(role);
        resource.setCapacityPerDay(capacityPerDay);
        resource.setCostPerDay(costPerDay);
        resource.setActive(true);
        return resource;
    }

    private static void assign(Task task, ProjectResource resource, String role, double allocation, double plannedHours, double actualHours) {
        if (task == null || resource == null) {
            return;
        }
        TaskResourceAssignment assignment = new TaskResourceAssignment();
        assignment.setResourceId(resource.getId());
        assignment.setResourceName(resource.getName());
        assignment.setRole(role);
        assignment.setAllocation(allocation);
        assignment.setPlannedHours(plannedHours);
        assignment.setActualHours(actualHours);
        task.getResourceAssignments().add(assignment);
    }

    private static BudgetItem budget(Task task, String name, String category, double planned, double actual,
                                     LocalDate plannedDate, LocalDate actualDate, String status) {
        BudgetItem item = new BudgetItem();
        item.setTaskPosition(task == null ? "" : task.getPosition());
        item.setName(name);
        item.setCategory(category);
        item.setPlannedAmount(planned);
        item.setActualAmount(actual);
        item.setPlannedDate(plannedDate);
        item.setActualDate(actualDate);
        item.setStatus(status);
        return item;
    }
}
