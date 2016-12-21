package fr.xephi.authme.process.register;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.register.executors.RegistrationExecutor;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link AsyncRegister}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncRegisterTest {

    @InjectMocks
    private AsyncRegister asyncRegister;

    @Mock
    private PlayerCache playerCache;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private CommonService commonService;
    @Mock
    private DataSource dataSource;

    @Test
    public void shouldDetectAlreadyLoggedInPlayer() {
        // given
        String name = "robert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);

        // when
        asyncRegister.register(player, executor);

        // then
        verify(commonService).send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
        verifyZeroInteractions(dataSource, executor);
    }

    @Test
    public void shouldStopForDisabledRegistration() {
        // given
        String name = "albert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(false);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);

        // when
        asyncRegister.register(player, executor);

        // then
        verify(commonService).send(player, MessageKey.REGISTRATION_DISABLED);
        verifyZeroInteractions(dataSource, executor);
    }

    @Test
    public void shouldStopForAlreadyRegisteredName() {
        // given
        String name = "dilbert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(true);
        given(dataSource.isAuthAvailable(name)).willReturn(true);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);

        // when
        asyncRegister.register(player, executor);

        // then
        verify(commonService).send(player, MessageKey.NAME_ALREADY_REGISTERED);
        verify(dataSource, only()).isAuthAvailable(name);
        verifyZeroInteractions(executor);
    }

    @Test
    public void shouldStopForFailedExecutorCheck() {
        // given
        String name = "edbert";
        Player player = mockPlayerWithName(name);
        TestHelper.mockPlayerIp(player, "33.44.55.66");
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(true);
        given(commonService.getProperty(RestrictionSettings.MAX_REGISTRATION_PER_IP)).willReturn(0);
        given(dataSource.isAuthAvailable(name)).willReturn(false);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);
        given(executor.isRegistrationAdmitted()).willReturn(false);

        // when
        asyncRegister.register(player, executor);

        // then
        verify(dataSource, only()).isAuthAvailable(name);
        verify(executor, only()).isRegistrationAdmitted();
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
