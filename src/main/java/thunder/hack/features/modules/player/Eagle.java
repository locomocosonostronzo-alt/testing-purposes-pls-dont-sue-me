package thunder.hack.features.modules.player;

import thunder.hack.events.MovementInputEvent;
import thunder.hack.features.modules.Category;
import thunder.hack.features.modules.ClientModule;
import thunder.hack.features.modules.ModuleManager;
import thunder.hack.utility.Utils;
import thunder.hack.utility.interfaces.Helper;

public class ModuleEagle extends ClientModule implements Helper {

    private static final float EDGE_MIN = 0.4f;
    private static final float EDGE_MAX = 0.6f;
    private static final float PITCH_MIN = -90f;
    private static final float PITCH_MAX = 90f;

    // Configurable fields
    private float edgeDistance;
    private float currentEdgeDistance;
    private boolean wasSneaking = false;
    private boolean conditionalEnabled = true;
    private float pitchMin = PITCH_MIN;
    private float pitchMax = PITCH_MAX;

    private Conditions[] activeConditions = new Conditions[]{Conditions.ON_GROUND};

    public ModuleEagle() {
        super("Eagle", Category.PLAYER);
        setAliases("FastBridge", "BridgeAssistant", "LegitScaffold");

        edgeDistance = randomEdgeDistance();
        currentEdgeDistance = edgeDistance;

        // Add ClickGUI sliders / toggles
        addSettings(
            new FloatSetting("EdgeDistance", edgeDistance, EDGE_MIN, EDGE_MAX),
            new FloatSetting("PitchMin", pitchMin, PITCH_MIN, PITCH_MAX),
            new FloatSetting("PitchMax", pitchMax, PITCH_MIN, PITCH_MAX),
            new BooleanSetting("ConditionalEnabled", conditionalEnabled)
        );

        // Register this module’s event handler with your mod’s event bus
        // Example: EventManager.register(this::handleMovementInput);
    }

    private float randomEdgeDistance() {
        return EDGE_MIN + (float) Math.random() * (EDGE_MAX - EDGE_MIN);
    }

    @Override
    public void onDisabled() {
        wasSneaking = false;
        super.onDisabled();
    }

    public void handleMovementInput(MovementInputEvent event) {
        // Update currentEdgeDistance from ClickGUI slider
        currentEdgeDistance = edgeDistance;

        boolean shouldBeActive = !player.abilities.flying
                && conditionalEnabled && shouldSneak(event)
                && Utils.isCloseToEdge(player, event.directionalInput, (double) currentEdgeDistance);

        event.sneak = (event.sneak && !shouldSneak(event)) || shouldBeActive;

        if (event.sneak) {
            wasSneaking = true;
        } else if (wasSneaking) {
            currentEdgeDistance = randomEdgeDistance();
            wasSneaking = false;
        }
    }

    private boolean shouldSneak(MovementInputEvent event) {
        boolean pitchOk = player.rotationPitch >= pitchMin && player.rotationPitch <= pitchMax;

        for (Conditions c : activeConditions) {
            if (!c.meetsCondition(event)) return false;
        }

        return pitchOk;
    }

    private enum Conditions {
        LEFT {
            @Override
            boolean meetsCondition(MovementInputEvent event) { return event.directionalInput.left; }
        },
        RIGHT {
            @Override
            boolean meetsCondition(MovementInputEvent event) { return event.directionalInput.right; }
        },
        FORWARDS {
            @Override
            boolean meetsCondition(MovementInputEvent event) { return event.directionalInput.forwards; }
        },
        BACKWARDS {
            @Override
            boolean meetsCondition(MovementInputEvent event) { return event.directionalInput.backwards; }
        },
        HOLDING_BLOCKS {
            @Override
            boolean meetsCondition(MovementInputEvent event) {
                return Utils.isValidBlock(player.getMainHandStack()) || Utils.isValidBlock(player.getOffHandStack());
            }
        },
        ON_GROUND {
            @Override
            boolean meetsCondition(MovementInputEvent event) { return player.isOnGround(); }
        },
        SNEAK {
            @Override
            boolean meetsCondition(MovementInputEvent event) { return event.sneak; }
        };

        abstract boolean meetsCondition(MovementInputEvent event);
    }
}
