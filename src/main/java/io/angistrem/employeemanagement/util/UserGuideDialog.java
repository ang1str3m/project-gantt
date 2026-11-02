package io.angistrem.employeemanagement.util;

import io.angistrem.employeemanagement.model.Settings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

public final class UserGuideDialog {

    private UserGuideDialog() {
    }

    public static void show(Window owner) {
        boolean dark = isDarkTheme();

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Руководство пользователя");
        dialog.setHeaderText(null);
        if (owner != null) {
            dialog.initOwner(owner);
        }

        TabPane tabs = new TabPane(
                buildTab(dark, "Старт",
                        heroCard(dark,
                                "Gantrem — рабочее пространство руководителя проекта",
                                "Руководство ниже построено не как справочник по кнопкам, а как последовательность реальных сценариев: как создать проект, собрать структуру, настроить сроки, проверить ресурсы, посмотреть деньги и подготовить печать.",
                                "Если вы открыли приложение впервые, начните именно с этой вкладки."),
                        quickSteps(dark),
                        cardGrid(dark,
                                miniCard(dark, "Пример первого запуска", "Файл → Новый проект → введите «Строительство завода», автора и сохраните проект в рабочую папку. После этого создайте 4–6 крупных этапов верхнего уровня."),
                                miniCard(dark, "Что должно получиться", "После первого шага у вас уже должна быть верхняя структура: проектирование, закупки, стройка, инженерия, пусконаладка и финальная веха."),
                                miniCard(dark, "Когда использовать демо", "Демо-проект удобен для обучения, презентации заказчику и проверки печати, анализа, drag&drop, ресурсов и бюджета."),
                                miniCard(dark, "Хорошая практика", "Сначала соберите проект сверху вниз. Не начинайте с десятков мелких подзадач, пока не понятны основные этапы и финишная веха.")
                        ),
                        callout(dark, "Быстрый ориентир", "Если нужно показать проект руководителю уже сегодня: откройте демо-проект, включите критический путь, затем «Анализ проекта», после этого проверьте «Сводку по этапам» и только потом переходите к печати или PDF.")
                ),
                buildTab(dark, "Структура проекта",
                        heroCard(dark,
                                "Как собирать проект правильно",
                                "В Gantrem есть четыре базовых уровня: группа задач, задача, подзадача и веха. Правильная структура сразу делает диаграмму чище, а аналитику — полезнее.",
                                "Ниже — не теория, а практические шаблоны построения проекта."),
                        section(dark, "Группа задач", "Группа нужна для крупного блока, например «Строительно-монтажные работы». Даты группы не задаются вручную: начало берётся по самой ранней задаче внутри, окончание — по самой поздней."),
                        section(dark, "Задача", "Задача — это самостоятельный этап с исполнителем, датами, статусом, приоритетом и зависимостями. Пример: «Монтаж металлокаркаса» или «Разработка рабочей документации»."),
                        section(dark, "Подзадача", "Подзадача помогает детализировать этап без перегруза верхнего уровня. Пример: внутри задачи «Фундаменты» можно держать подзадачи «Котлован», «Армирование», «Бетонирование»."),
                        section(dark, "Веха", "Веха — контрольная точка без длительности. Пример: «ЗОС получено», «Линия под напряжением», «Запуск завода». Для вехи достаточно одной даты."),
                        cardGrid(dark,
                                miniCard(dark, "Пример хорошей структуры", "Группа «Технологическая часть» → задача «Монтаж линии» → подзадачи «Поставка», «Сборка», «Подключение», «ПНР»."),
                                miniCard(dark, "Пример плохой структуры", "Сразу 70 верхнеуровневых задач без групп и без вех. Такой проект трудно читать, печатать и объяснять на встречах."),
                                miniCard(dark, "Drag & Drop", "Задачи можно переносить мышкой выше, ниже, в группу, между группами и обратно в корень. Подзадачи тоже можно переставлять и переносить в другие задачи."),
                                miniCard(dark, "Нумерация", "Нумерация пересчитывается автоматически после перестановок. Это значит, что структура дерева должна быть логичной, а не только красивой визуально.")
                        )
                ),
                buildTab(dark, "Планирование",
                        heroCard(dark,
                                "Сроки, связи и тактильная работа мышкой",
                                "После того как собрана структура, проект становится живым через зависимости, лаги, финансовые ограничения и ручное управление мышкой на диаграмме.",
                                "На этой вкладке собраны самые частые рабочие сценарии."),
                        legendGroup(dark, "Как читать диаграмму",
                                legendRow(dark, sampleMainBar(), "Основная задача", "Крупный этап работ. Пример: «Монтаж инженерных систем»."),
                                legendRow(dark, sampleSubBar(), "Подзадача", "Детализация внутри задачи. Пример: «Монтаж щитов», «Прокладка кабеля», «Пусконаладка»."),
                                legendRow(dark, sampleMilestone(), "Веха", "Контрольная дата без длительности. Хорошо подходит для сдачи, оплат и запусков."),
                                legendRow(dark, sampleRiskBadge(), "Рисковый объект", "Элемент, который уже давит на график и требует внимания.")
                        ),
                        legendGroup(dark, "Зависимости и лаги",
                                legendRow(dark, sampleLine("classic", "#7c848f"), "FS", "Следующий этап стартует после завершения предыдущего. Пример: «Фундаменты» → «Каркас»."),
                                legendRow(dark, sampleLine("rounded", "#2563eb"), "SS", "Два этапа стартуют почти одновременно. Пример: параллельная подготовка документации и закупки."),
                                legendRow(dark, sampleLine("metro", "#0f766e"), "FF / SF", "Используются, когда важна синхронизация завершения или редкие технологические сценарии."),
                                section(dark, "Пример лага", "Связь FS +5 означает, что следующая задача стартует через 5 дней после завершения предыдущей. Лаг удобно использовать для выдержки бетона, поставки, согласования или мобилизации подрядчика.")
                        ),
                        section(dark, "Работа мышкой", "Задачу можно тянуть целиком, менять начало и конец за края, а веху — двигать как одну точку. Это удобно для быстрых совещаний, когда план надо уточнить буквально на глазах у команды."),
                        callout(dark, "Рабочий сценарий", "Откройте масштаб «Неделя», выделите проблемный этап, включите связи и подвигайте задачу мышкой. После этого нажмите «Пересчитать» и сразу посмотрите, как сдвинулась финальная веха.")
                ),
                buildTab(dark, "Ресурсы и бюджет",
                        heroCard(dark,
                                "Сроки без ресурсов и денег — неполная картина",
                                "Gantrem уже умеет вести справочник ресурсов, назначать их на задачи, считать перегруз, показывать бюджет по статьям и собирать платёжный календарь.",
                                "Эти инструменты особенно полезны, когда проект уже собран и нужно понять, где реально горит."),
                        section(dark, "Ресурсы", "Откройте Редактирование → Ресурсы и заведите людей, бригады, подрядчиков или технику. Затем откройте задачу и назначьте на неё ресурс с загрузкой и трудозатратами."),
                        section(dark, "Анализ загрузки", "Редактирование → Загрузка ресурсов покажет, кто перегружен, где пик нагрузки и какие задачи конфликтуют по датам. В таблице и на диаграмме такие задачи подсвечиваются."),
                        section(dark, "Бюджет", "Редактирование → Бюджет позволяет привязать статьи затрат к задачам и этапам. Вы можете вести план, факт, дату платежа и статус статьи."),
                        section(dark, "Платёжный календарь", "Редактирование → Платёжный календарь собирает месяцы с пиками платежей и показывает дорогие этапы, чтобы видеть самые тяжёлые участки проекта."),
                        cardGrid(dark,
                                miniCard(dark, "Пример ресурса", "Создайте ресурс «Главный инженер», назначьте его на задачи проектирования и ввода. Если одни и те же даты пересекутся, система покажет перегруз."),
                                miniCard(dark, "Пример бюджета", "Для этапа «Каркас и оболочка» добавьте статьи «Металлоконструкции», «Монтаж», «Подъёмная техника». После этого будет виден план, факт и отклонение по этапу."),
                                miniCard(dark, "Как читать сигнал", "Оранжевый акцент в задаче — перегруз ресурса. Фиолетовый — бюджетное давление. Если задача горит и по ресурсам, и по деньгам, она становится ещё заметнее."),
                                miniCard(dark, "Что делать дальше", "После обнаружения перегруза или перерасхода откройте этап в «Сводке по этапам» и посмотрите, что именно тащит риск: сроки, деньги или ресурсы.")
                        )
                ),
                buildTab(dark, "Аналитика",
                        heroCard(dark,
                                "Главный вопрос: что именно двигает финиш проекта",
                                "В Gantrem уже есть не только диаграмма, но и управленческая аналитика: критический путь, анализ проекта, сводка по этапам и индикатор риска.",
                                "Это вкладка для руководителя проекта, а не только для планировщика."),
                        section(dark, "Критический путь", "Кнопка «Критический путь» показывает цепочку задач и связей, которая сильнее всего влияет на конечную дату. Если здесь есть перегруженные или финансово заблокированные задачи, риск особенно высок."),
                        section(dark, "Анализ проекта", "Кнопка «Анализ проекта» отвечает на вопрос: какая задача сильнее всего двигает финальную веху, кто её исполнитель и на сколько дней сдвинется финиш при задержке на 1 или 3 дня."),
                        section(dark, "Сводка по этапам", "Редактирование → Сводка по этапам показывает проект уже по крупным блокам: где много критических задач, где просрочка, где перегруз ресурсов и где перерасход бюджета."),
                        section(dark, "Индикатор риска", "Число рядом с «Риски проекта» — это быстрый индекс напряжённости плана. Клик по нему открывает сводку по этапам."),
                        callout(dark, "Пример разговора с руководителем", "«Финишная веха ЗОС сейчас зависит от этапа “Каркас и оболочка”. При задержке задачи “Монтаж колонн” на 3 дня мы теряем 3 дня по финишу. По этому же этапу уже есть перегруз ресурса и перерасход бюджета». Такой сценарий можно показать прямо в интерфейсе без отдельных расчётов.")
                ),
                buildTab(dark, "Печать и экспорт",
                        heroCard(dark,
                                "Подготовка проекта к демонстрации, отчёту и PDF",
                                "Экспорт в Gantrem уже построен вокруг реального текущего вида: что видно в таблице и на диаграмме, то и поедет в печать. Поэтому качество результата зависит от того, как вы подготовили экран.",
                                "Ниже собраны режимы и практические примеры."),
                        section(dark, "Как готовить проект к печати", "Перед печатью откройте нужные группы, выберите удобный масштаб, проверьте видимость зависимостей и убедитесь, что на экране показан именно тот срез проекта, который вы хотите вынести в PDF."),
                        section(dark, "Пример для руководителя", "Если нужен обзор на совещание, обычно лучше подходит масштаб «По ширине» или «На один лист» на A3/A2. Если нужно разбирать конкретный месяц, откройте масштаб «Месяц» или «Неделя»."),
                        section(dark, "Режимы масштаба", "Авто размер страницы сохраняет проект максимально близко к экрану. Текущий масштаб печатает то, что вы видите сейчас. Уместить по ширине сохраняет ширину и разбивает длинный проект вниз. Уместить по высоте делает наоборот. Уместить на один лист максимально заполняет выбранный формат бумаги."),
                        scaleModeVisuals(dark),
                        cardGrid(dark,
                                miniCard(dark, "Когда использовать Авто", "Когда уже подобран идеальный вид проекта и вы хотите получить PDF почти один в один с экраном."),
                                miniCard(dark, "Когда использовать По ширине", "Когда проект длинный по срокам и нужен читаемый обзор на нескольких листах, а не микроскопический масштаб."),
                                miniCard(dark, "Когда использовать По высоте", "Когда важнее сохранить вертикальную читаемость дерева задач и разбить проект вправо по временной оси."),
                                miniCard(dark, "Когда использовать На один лист", "Когда нужен общий обзор проекта на одном формате A4/A3/A2/A1 и допустимо уменьшение масштаба.")
                        ),
                        section(dark, "CSV", "CSV экспортирует табличные данные для Excel, BI-систем или передачи в смежные процессы.")
                ),
                buildTab(dark, "FAQ",
                        faq(dark, "Почему задача не двигается автоматически?", "Проверьте зависимости, лаги, финансовые ограничения и то, не была ли задача недавно сдвинута вручную."),
                        faq(dark, "Почему группа сама меняет даты?", "Так и задумано: группа не задаёт свои даты вручную, а собирает их автоматически по задачам внутри."),
                        faq(dark, "Почему задача подсветилась оранжевым?", "Это сигнал о перегрузе ресурса. Откройте «Загрузка ресурсов» и посмотрите, какие даты и задачи конфликтуют."),
                        faq(dark, "Почему задача подсветилась фиолетовым?", "Это сигнал бюджетного давления: по связанной статье или этапу факт уже выше плана."),
                        faq(dark, "Как вернуть удалённую задачу?", "Используйте Ctrl+Z."),
                        faq(dark, "Что делать после сбоя?", "При следующем запуске приложение предложит восстановить автосохранённый проект."),
                        faq(dark, "С чего лучше начинать обучение команды?", "Откройте демо-проект, покажите структуру групп, затем критический путь, анализ проекта, ресурсы, бюджет и только после этого печать/PDF.")
                )
        );

        tabs.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
        tabs.setPrefSize(1120, 810);
        tabs.setStyle("-fx-tab-min-width: 132px; -fx-tab-max-width: 186px;"
                + "-fx-tab-min-height: 40px;"
                + "-fx-padding: 10 10 0 10;"
                + "-fx-background-color: " + (dark ? "#0b1220" : "#eef3f8") + ";");

        dialog.getDialogPane().setContent(tabs);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().setPrefSize(1180, 870);
        SceneManager.applyThemeToDialog(dialog);
        dialog.showAndWait();
    }

    private static Tab buildTab(boolean dark, String title, Node... blocks) {
        VBox content = new VBox(16);
        content.setPadding(new Insets(20, 22, 26, 22));
        content.setPrefWidth(1030);
        content.setMaxWidth(1030);
        content.setStyle("-fx-background-color: " + (dark ? "#0f172a" : "#f8fafc") + ";");
        content.getChildren().addAll(blocks);

        StackPane centered = new StackPane(content);
        centered.setPadding(new Insets(0, 0, 0, 0));
        centered.setAlignment(Pos.TOP_CENTER);
        centered.setStyle("-fx-background-color: " + (dark ? "#0f172a" : "#f8fafc") + ";");

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setContent(centered);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(false);
        scrollPane.setPannable(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(dark
                ? "-fx-background-color: #0f172a; -fx-background: #0f172a;"
                : "-fx-background-color: #f8fafc; -fx-background: #f8fafc;");

        return new Tab(title, scrollPane);
    }

    private static VBox heroCard(boolean dark, String title, String subtitle, String footnote) {
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 900; -fx-text-fill: "
                + (dark ? "#ffffff" : "#0f172a") + ";");

        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setWrapText(true);
        subtitleLabel.setStyle("-fx-font-size: 13px; -fx-line-spacing: 3px; -fx-text-fill: "
                + (dark ? "#eff6ff" : "#1e293b") + ";");

        Label footnoteLabel = new Label(footnote);
        footnoteLabel.setWrapText(true);
        footnoteLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (dark ? "rgba(255,255,255,0.82)" : "#334155") + ";");

        VBox box = new VBox(8, titleLabel, subtitleLabel, footnoteLabel);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: "
                + (dark ? "linear-gradient(to right, #1d4ed8, #0f766e)" : "linear-gradient(to right, #dbeafe, #dcfce7)") + ";"
                + "-fx-background-radius: 20; -fx-border-radius: 20;"
                + "-fx-border-color: " + (dark ? "rgba(191,219,254,0.16)" : "rgba(96,165,250,0.16)") + ";"
                + "-fx-border-radius: 20;");
        return box;
    }

    private static VBox section(boolean dark, String title, String text) {
        return textBlock(dark, title, text);
    }

    private static VBox faq(boolean dark, String title, String text) {
        return textBlock(dark, title, text);
    }

    private static VBox textBlock(boolean dark, String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: "
                + (dark ? "#f8fafc" : "#0f172a") + ";");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 13px; -fx-line-spacing: 3px; -fx-text-fill: "
                + (dark ? "#dbe4f0" : "#334155") + ";");

        VBox box = new VBox(7, titleLabel, textLabel);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: " + (dark ? "rgba(30,41,59,0.95)" : "rgba(255,255,255,0.96)") + ";"
                + "-fx-background-radius: 16; -fx-border-radius: 16;"
                + "-fx-border-color: " + (dark ? "rgba(100,116,139,0.34)" : "rgba(148,163,184,0.22)") + ";"
                + "-fx-border-width: 1;"
                + "-fx-effect: dropshadow(gaussian, " + (dark ? "rgba(2,6,23,0.20)" : "rgba(15,23,42,0.05)") + ", 18, 0.12, 0, 2);");
        return box;
    }

    private static VBox callout(boolean dark, String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 900; -fx-text-fill: "
                + (dark ? "#fde68a" : "#92400e") + ";");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 12px; -fx-line-spacing: 2px; -fx-text-fill: "
                + (dark ? "#fef3c7" : "#78350f") + ";");

        VBox box = new VBox(6, titleLabel, textLabel);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: " + (dark ? "rgba(120,53,15,0.28)" : "rgba(254,243,199,0.92)") + ";"
                + "-fx-background-radius: 16; -fx-border-radius: 16;"
                + "-fx-border-color: " + (dark ? "rgba(245,158,11,0.32)" : "rgba(217,119,6,0.24)") + ";"
                + "-fx-border-width: 1;");
        return box;
    }

    private static GridPane cardGrid(boolean dark, Node... cards) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        for (int i = 0; i < cards.length; i++) {
            Node card = cards[i];
            int col = i % 2;
            int row = i / 2;
            grid.add(card, col, row);
            GridPane.setHgrow(card, Priority.ALWAYS);
        }
        return grid;
    }

    private static VBox miniCard(boolean dark, String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: "
                + (dark ? "#f8fafc" : "#0f172a") + ";");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 12px; -fx-line-spacing: 2px; -fx-text-fill: "
                + (dark ? "#dbe4f0" : "#475569") + ";");

        VBox box = new VBox(6, titleLabel, textLabel);
        box.setPadding(new Insets(14));
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: " + (dark ? "rgba(30,41,59,0.95)" : "rgba(255,255,255,0.96)") + ";"
                + "-fx-background-radius: 16; -fx-border-radius: 16;"
                + "-fx-border-color: " + (dark ? "rgba(100,116,139,0.34)" : "rgba(148,163,184,0.22)") + ";"
                + "-fx-border-width: 1;");
        return box;
    }

    private static GridPane scaleModeVisuals(boolean dark) {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);

        VBox auto = scaleModeCard(dark, "Авто размер страницы", "Страница строится под реальный размер текущего вида проекта.", "auto");
        VBox fitWidth = scaleModeCard(dark, "Уместить по ширине", "Вся ширина входит в лист. Если проект высокий, он продолжается на следующих страницах вниз.", "fit-width");
        VBox fitHeight = scaleModeCard(dark, "Уместить по высоте", "Вся высота входит в лист. Если проект слишком широкий, он продолжается вправо на следующих страницах.", "fit-height");
        VBox fitPage = scaleModeCard(dark, "Уместить на один лист", "Вся таблица и диаграмма сжимаются под один лист выбранного формата.", "fit-page");

        auto.setMaxWidth(Double.MAX_VALUE);
        fitWidth.setMaxWidth(Double.MAX_VALUE);
        fitHeight.setMaxWidth(Double.MAX_VALUE);
        fitPage.setMaxWidth(Double.MAX_VALUE);

        grid.add(auto, 0, 0);
        grid.add(fitWidth, 1, 0);
        grid.add(fitHeight, 0, 1);
        grid.add(fitPage, 1, 1);

        GridPane.setHgrow(auto, Priority.ALWAYS);
        GridPane.setHgrow(fitWidth, Priority.ALWAYS);
        GridPane.setHgrow(fitHeight, Priority.ALWAYS);
        GridPane.setHgrow(fitPage, Priority.ALWAYS);

        return grid;
    }

    private static VBox scaleModeCard(boolean dark, String title, String text, String mode) {
        StackPane preview = scaleModePreview(dark, mode);
        Label titleLabel = new Label(title);
        titleLabel.setWrapText(true);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: "
                + (dark ? "#f8fafc" : "#0f172a") + ";");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 12px; -fx-line-spacing: 2px; -fx-text-fill: "
                + (dark ? "#dbe4f0" : "#475569") + ";");

        VBox box = new VBox(10, preview, titleLabel, textLabel);
        box.setPadding(new Insets(14));
        box.setMinWidth(0);
        box.setMaxWidth(Double.MAX_VALUE);
        box.setStyle("-fx-background-color: " + (dark ? "rgba(30,41,59,0.95)" : "rgba(255,255,255,0.96)") + ";"
                + "-fx-background-radius: 16; -fx-border-radius: 16;"
                + "-fx-border-color: " + (dark ? "rgba(100,116,139,0.34)" : "rgba(148,163,184,0.22)") + ";"
                + "-fx-border-width: 1;");
        return box;
    }

    private static StackPane scaleModePreview(boolean dark, String mode) {
        Pane canvas = new Pane();
        canvas.setPrefSize(200, 120);

        Color pageStroke = dark ? Color.web("#64748b") : Color.web("#94a3b8");
        Color pageFill = dark ? Color.web("#0f172a") : Color.web("#ffffff");
        Color contentFill = dark ? Color.web("#2563eb", 0.90) : Color.web("#3b82f6", 0.88);
        Color contentFillMuted = dark ? Color.web("#60a5fa", 0.45) : Color.web("#93c5fd", 0.65);
        Color accent = dark ? Color.web("#f59e0b") : Color.web("#d97706");

        Rectangle page = new Rectangle(136, 88);
        page.setArcWidth(14);
        page.setArcHeight(14);
        page.setX(28);
        page.setY(16);
        page.setFill(pageFill);
        page.setStroke(pageStroke);

        Rectangle content = new Rectangle(108, 58);
        content.setArcWidth(8);
        content.setArcHeight(8);
        content.setX(42);
        content.setY(30);
        content.setFill(contentFill);

        canvas.getChildren().add(page);

        switch (mode) {
            case "auto" -> {
                content.setWidth(112);
                content.setHeight(62);
                canvas.getChildren().add(content);
            }
            case "fit-width" -> {
                Rectangle lowerPage = new Rectangle(136, 34);
                lowerPage.setArcWidth(14);
                lowerPage.setArcHeight(14);
                lowerPage.setX(28);
                lowerPage.setY(88);
                lowerPage.setFill(pageFill);
                lowerPage.setStroke(pageStroke);

                Rectangle topSlice = new Rectangle(108, 46);
                topSlice.setArcWidth(8);
                topSlice.setArcHeight(8);
                topSlice.setX(42);
                topSlice.setY(30);
                topSlice.setFill(contentFill);

                Rectangle bottomSlice = new Rectangle(108, 18);
                bottomSlice.setArcWidth(8);
                bottomSlice.setArcHeight(8);
                bottomSlice.setX(42);
                bottomSlice.setY(95);
                bottomSlice.setFill(contentFillMuted);

                Label flow = new Label("↓ 2 стр.");
                flow.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + toHex(accent) + ";");
                flow.setLayoutX(146);
                flow.setLayoutY(92);

                canvas.getChildren().addAll(topSlice, lowerPage, bottomSlice, flow);
            }
            case "fit-height" -> {
                Rectangle rightPage = new Rectangle(56, 88);
                rightPage.setArcWidth(14);
                rightPage.setArcHeight(14);
                rightPage.setX(144);
                rightPage.setY(16);
                rightPage.setFill(pageFill);
                rightPage.setStroke(pageStroke);

                Rectangle leftSlice = new Rectangle(96, 58);
                leftSlice.setArcWidth(8);
                leftSlice.setArcHeight(8);
                leftSlice.setX(42);
                leftSlice.setY(30);
                leftSlice.setFill(contentFill);

                Rectangle rightSlice = new Rectangle(34, 58);
                rightSlice.setArcWidth(8);
                rightSlice.setArcHeight(8);
                rightSlice.setX(155);
                rightSlice.setY(30);
                rightSlice.setFill(contentFillMuted);

                Label flow = new Label("→ 2 стр.");
                flow.setStyle("-fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + toHex(accent) + ";");
                flow.setLayoutX(146);
                flow.setLayoutY(92);

                canvas.getChildren().addAll(leftSlice, rightPage, rightSlice, flow);
            }
            case "fit-page" -> {
                Rectangle scaled = new Rectangle(88, 42);
                scaled.setArcWidth(8);
                scaled.setArcHeight(8);
                scaled.setX(52);
                scaled.setY(38);
                scaled.setFill(contentFill);
                canvas.getChildren().add(scaled);
            }
            default -> canvas.getChildren().add(content);
        }

        StackPane wrapper = new StackPane(canvas);
        wrapper.setPadding(new Insets(6));
        wrapper.setStyle("-fx-background-color: " + (dark ? "rgba(15,23,42,0.85)" : "rgba(248,250,252,0.92)") + ";"
                + "-fx-background-radius: 14; -fx-border-radius: 14;"
                + "-fx-border-color: " + (dark ? "rgba(100,116,139,0.26)" : "rgba(148,163,184,0.18)") + ";"
                + "-fx-border-width: 1;");
        return wrapper;
    }

    private static String toHex(Color color) {
        return String.format("#%02x%02x%02x",
                (int) Math.round(color.getRed() * 255),
                (int) Math.round(color.getGreen() * 255),
                (int) Math.round(color.getBlue() * 255));
    }

    private static VBox quickSteps(boolean dark) {
        return legendGroup(dark, "Быстрый маршрут по приложению",
                quickStep(dark, "1", "Создайте проект", "Файл → Новый проект. Укажите название проекта и автора."),
                quickStep(dark, "2", "Соберите структуру", "Добавьте основные задачи, затем при необходимости раскройте их на подзадачи."),
                quickStep(dark, "3", "Поставьте связи", "Задайте FS / SS / FF / SF, лаги и при необходимости финансовые ограничения."),
                quickStep(dark, "4", "Пересчитайте", "Нажмите «Пересчитать», чтобы график учел зависимости и финансовые маркеры."),
                quickStep(dark, "5", "Покажите результат", "Выберите масштаб, включите критический путь или анализ проекта и подготовьте печать.")
        );
    }

    private static HBox quickStep(boolean dark, String number, String title, String text) {
        Circle circle = new Circle(15, Color.web(dark ? "#60a5fa" : "#2563eb"));
        Label n = new Label(number);
        n.setStyle("-fx-font-size: 11px; -fx-font-weight: 900; -fx-text-fill: white;");
        StackPane marker = new StackPane(circle, n);
        marker.setMinSize(30, 30);

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: "
                + (dark ? "#f8fafc" : "#0f172a") + ";");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: "
                + (dark ? "#dbe4f0" : "#475569") + ";");

        VBox textBox = new VBox(2, titleLabel, textLabel);
        HBox row = new HBox(12, marker, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle("-fx-background-color: " + (dark ? "rgba(15,23,42,0.58)" : "rgba(241,245,249,0.9)") + ";"
                + "-fx-background-radius: 12; -fx-border-radius: 12;"
                + "-fx-border-color: " + (dark ? "rgba(100,116,139,0.18)" : "rgba(148,163,184,0.16)") + ";");
        return row;
    }

    private static VBox legendGroup(boolean dark, String title, Node... rows) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: "
                + (dark ? "#f8fafc" : "#0f172a") + ";");

        VBox box = new VBox(10, titleLabel);
        box.getChildren().addAll(rows);
        box.setPadding(new Insets(14));
        box.setStyle("-fx-background-color: " + (dark ? "rgba(30,41,59,0.95)" : "rgba(255,255,255,0.96)") + ";"
                + "-fx-background-radius: 16; -fx-border-radius: 16;"
                + "-fx-border-color: " + (dark ? "rgba(100,116,139,0.34)" : "rgba(148,163,184,0.22)") + ";"
                + "-fx-border-width: 1;");
        return box;
    }

    private static HBox legendRow(boolean dark, Node sample, String title, String text) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: "
                + (dark ? "#f8fafc" : "#0f172a") + ";");

        Label textLabel = new Label(text);
        textLabel.setWrapText(true);
        textLabel.setStyle("-fx-font-size: 12px; -fx-line-spacing: 2px; -fx-text-fill: "
                + (dark ? "#dbe4f0" : "#475569") + ";");

        VBox textBox = new VBox(3, titleLabel, textLabel);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        HBox row = new HBox(12, sample, textBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private static Node sampleMainBar() {
        Pane pane = new Pane();
        pane.setPrefSize(76, 18);
        Rectangle bar = new Rectangle(4, 4, 64, 10);
        bar.setArcWidth(8);
        bar.setArcHeight(8);
        bar.setFill(Color.web("#2563eb"));
        pane.getChildren().add(bar);
        return pane;
    }

    private static Node sampleSubBar() {
        Pane pane = new Pane();
        pane.setPrefSize(64, 16);
        Rectangle bar = new Rectangle(8, 4, 46, 8);
        bar.setArcWidth(6);
        bar.setArcHeight(6);
        bar.setFill(Color.web("#16a34a"));
        pane.getChildren().add(bar);
        return pane;
    }

    private static Node sampleMilestone() {
        Pane pane = new Pane();
        pane.setPrefSize(32, 24);
        Polygon diamond = new Polygon(16.0, 3.0, 28.0, 12.0, 16.0, 21.0, 4.0, 12.0);
        diamond.setFill(Color.web("#2563eb"));
        pane.getChildren().add(diamond);
        return pane;
    }

    private static Node sampleRiskBadge() {
        Label label = new Label("RISK");
        label.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #92400e;"
                + "-fx-background-color: #fef3c7; -fx-padding: 3 8 3 8; -fx-background-radius: 12;");
        return label;
    }

    private static Node sampleMarker(String text, String color) {
        Pane pane = new Pane();
        pane.setPrefSize(66, 28);
        Line line = new Line(18, 6, 18, 25);
        line.setStroke(Color.web(color));
        line.setStrokeWidth(2.2);
        Circle dot = new Circle(18, 6, 3.8, Color.web(color));

        Rectangle bg = new Rectangle(0, 0, 26, 14);
        bg.setArcWidth(10);
        bg.setArcHeight(10);
        bg.setFill(Color.web(color));

        Label textLabel = new Label(text);
        textLabel.setStyle("-fx-font-size: 9px; -fx-font-weight: 900; -fx-text-fill: white;");

        StackPane badge = new StackPane(bg, textLabel);
        badge.resizeRelocate(26, 0, 26, 14);
        pane.getChildren().addAll(line, dot, badge);
        return pane;
    }

    private static Node sampleLine(String style, String color) {
        Pane pane = new Pane();
        pane.setPrefSize(92, 30);
        Color stroke = Color.web(color);
        if ("classic".equals(style)) {
            Line l1 = new Line(8, 8, 52, 8);
            Line l2 = new Line(52, 8, 52, 22);
            Line l3 = new Line(52, 22, 82, 22);
            l1.setStroke(stroke);
            l2.setStroke(stroke);
            l3.setStroke(stroke);
            l1.setStrokeWidth(1.3);
            l2.setStrokeWidth(1.3);
            l3.setStrokeWidth(1.3);
            Polygon arrow = new Polygon(82, 22, 74, 18, 74, 26);
            arrow.setFill(stroke);
            pane.getChildren().addAll(l1, l2, l3, arrow);
            return pane;
        }

        CubicCurve curve = new CubicCurve(8, 8, 34, 8, 28, 22, 82, 22);
        curve.setFill(null);
        curve.setStroke(stroke);
        curve.setStrokeWidth("metro".equals(style) ? 1.2 : 1.5);
        Polygon arrow = new Polygon(82, 22, 74, 18, 74, 26);
        arrow.setFill(stroke);
        pane.getChildren().addAll(curve, arrow);
        return pane;
    }

    private static boolean isDarkTheme() {
        Settings settings = AppSettings.get();
        return settings != null
                && settings.getTheme() != null
                && "dark".equalsIgnoreCase(settings.getTheme().trim());
    }
}
