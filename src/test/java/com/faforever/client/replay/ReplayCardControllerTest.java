package com.faforever.client.replay;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PlayerStatsMapBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.builders.ReplayReviewBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import com.faforever.client.fx.FxApplicationThreadExecutor;
import com.faforever.client.game.PlayerCardController;
import com.faforever.commons.api.dto.Validity;
import java.time.OffsetDateTime;
import java.util.List;
import javafx.scene.image.Image;
import java.io.InputStream;

import javafx.scene.layout.Pane;

import org.mockito.Mock;
import static org.mockito.Mockito.lenient;

import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;
import org.mockito.InjectMocks;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import com.faforever.client.builders.MapBeanBuilder;
import com.faforever.client.builders.MapVersionBeanBuilder;
import com.faforever.client.builders.PlayerBeanBuilder;
import com.faforever.client.builders.PlayerStatsMapBuilder;
import com.faforever.client.builders.ReplayBeanBuilder;
import com.faforever.client.builders.ReplayReviewBeanBuilder;
import com.faforever.client.domain.FeaturedModBean;
import com.faforever.client.domain.MapVersionBean;
import com.faforever.client.domain.PlayerBean;
import com.faforever.client.domain.ReplayBean;
import com.faforever.client.domain.ReplayReviewBean;
import com.faforever.client.fx.ImageViewHelper;
import com.faforever.client.fx.contextmenu.ContextMenuBuilder;
import com.faforever.client.game.TeamCardController;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapService;
import com.faforever.client.map.MapService.PreviewSize;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.PlayerService;
import com.faforever.client.rating.RatingService;
import com.faforever.client.reporting.ReportDialogController;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.test.PlatformTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.util.TimeService;
import com.faforever.client.vault.review.ReviewController;
import com.faforever.client.vault.review.ReviewService;
import com.faforever.client.vault.review.ReviewsController;
import com.faforever.client.vault.review.StarController;
import com.faforever.client.vault.review.StarsController;
import com.faforever.commons.api.dto.Validity;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.testfx.util.WaitForAsyncUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.InputStream;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ReplayCardControllerTest extends PlatformTest {
    @InjectMocks
    private ReplayCardController instance;

    @Mock
    private UiService uiService;

    @Mock
    private ReplayService replayService;

    @Mock
    private TimeService timeService;

    @Mock
    private MapService mapService;

    @Mock
    private RatingService ratingService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ImageViewHelper imageViewHelper;

    @Mock
    private I18n i18n;

    @Mock
    private FxApplicationThreadExecutor fxApplicationThreadExecutor;

    @Mock
    private PlayerCardController playerCardController;

    private PlayerBean currentPlayer;
    private ReplayBean onlineReplay;
    private ReplayBean localReplay;
    private MapVersionBean mapBean;

    private final BooleanProperty installed = new SimpleBooleanProperty();

    @BeforeEach
    public void setUp() throws Exception {
        instance = new ReplayCardController();
        currentPlayer = PlayerBeanBuilder.create().defaultValues().get();
        mapBean = MapVersionBeanBuilder.create().defaultValues().map(MapBeanBuilder.create().defaultValues().get()).get();
        onlineReplay = ReplayBeanBuilder.create().defaultValues()
            .validity(Validity.VALID)
            .featuredMod(new FeaturedModBean())
            .title("test")
            .mapVersion(mapBean)
            .teamPlayerStats(PlayerStatsMapBuilder.create().defaultValues().get())
            .get();

        localReplay = ReplayBeanBuilder.create().defaultValues()
            .local(true)
            .validity(null)
            .featuredMod(new FeaturedModBean())
            .title("test")
            .replayFile(Path.of("foo.tmp"))
            .get();

        lenient().when(uiService.loadFxml("theme/player_card.fxml")).thenReturn(playerCardController);
        lenient().when(replayService.loadReplayDetails(any())).thenReturn(new ReplayDetails(List.of(), List.of(), mapBean));
        lenient().when(mapService.isInstalledBinding(Mockito.<MapVersionBean>any())).thenReturn(installed);
        lenient().when(mapService.loadPreview(anyString(), eq(PreviewSize.LARGE)))
         .thenReturn(new Image(InputStream.nullInputStream()));
        lenient().when(fxApplicationThreadExecutor.asScheduler()).thenReturn(Schedulers.immediate());
    }

    @Test
    public void setReplayOnline() {
        when(ratingService.calculateQuality(onlineReplay)).thenReturn(0.427);
        when(i18n.get(eq("percentage"), eq(Math.round(0.427 * 100)))).thenReturn("42");

        runOnFxThreadAndWait(() -> instance.setEntity(onlineReplay));

        //verify(mapService).loadPreview(mapBean, PreviewSize.SMALL);
        assertTrue(instance.teamsContainer.isVisible());
    }
}