package landmark.client;

import com.mojang.blaze3d.platform.InputConstants;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import landmark.LandmarkClient;
import landmark.data.ColorKey;
import landmark.data.ColorMath;
import landmark.data.ExportPreset;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/**
 * Settings: keybinds, feature toggles, export quality and every colour. Opened from Mod Menu (if installed) or from the
 * Settings button on the map. Changes apply immediately and are saved when the screen closes.
 */
public class ConfigScreen extends Screen {
	private static final int ROW_H = 24;

	private final Screen parent;
	private final LandmarkState state = LandmarkState.get();
	private ConfigList list;
	private double savedScroll;
	private @Nullable KeyMapping listening;

	public ConfigScreen(Screen parent) {
		super(Component.translatable("landmark.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		list = new ConfigList();
		addRenderableWidget(list);
		populate();
		list.setScrollAmount(savedScroll);
		int w = Math.min(width - 20, 340);
		int x = (width - w) / 2;
		addRenderableWidget(Button.builder(Component.translatable("landmark.colors.resetall"), b -> confirmResetAll())
			.bounds(x, height - 28, w / 2 - 2, 20).build());
		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
			.bounds(x + w / 2 + 2, height - 28, w / 2 - 2, 20).build());
	}

	/** Fills the list from the current settings. Called again whenever a setting changes the list's shape. */
	private void populate() {
		savedScroll = list.scrollAmount();
		list.clear();
		list.add(new HeaderRow(Component.translatable("landmark.config.keybinds")));
		list.add(new KeyRow(LandmarkClient.openKey));
		note(Component.translatable("landmark.config.keybinds.note"), Palette.get(ColorKey.TEXT_DIM));

		list.add(new HeaderRow(Component.translatable("landmark.config.features")));
		list.add(new ToggleRow(Component.translatable("landmark.config.teleportwarning"), () -> state.config.confirmTeleport, v -> state.config.confirmTeleport = v));
		list.add(new ToggleRow(Component.translatable("landmark.config.savespawns"), () -> state.config.saveSpawns, v -> state.config.saveSpawns = v));
		list.add(new SavedSpawnsRow());
		list.add(new ToggleRow(Component.translatable("landmark.config.outlineother"), () -> state.config.outlineOtherLands, v -> state.config.outlineOtherLands = v));
		list.add(new ToggleRow(Component.translatable("landmark.config.playermarker"), () -> state.config.showPlayerMarker, v -> state.config.showPlayerMarker = v));
		list.add(new ToggleRow(Component.translatable("landmark.config.refreshonopen"), () -> state.config.refreshOnOpen, v -> state.config.refreshOnOpen = v));

		list.add(new HeaderRow(Component.translatable("landmark.config.export")));
		list.add(new PresetRow());
		ExportPreset preset = state.config.exportPreset();
		note(Component.translatable("landmark.export.desc." + preset.id), Palette.get(ColorKey.TEXT_STATUS));
		if (preset.isHeavy()) {
			note(Component.translatable("landmark.export.warning"), Palette.get(ColorKey.LIST_OPEN));
		}

		ColorKey.Group group = null;
		list.add(new HeaderRow(Component.translatable("landmark.config.colors")));
		for (ColorKey key : ColorKey.values()) {
			if (key.group != group) {
				group = key.group;
				list.add(new SubHeaderRow(Component.translatable(group.translationKey)));
			}
			list.add(new ColorRow(key));
		}
	}

	/** Dev harness hooks. */
	void devDeleteSaved() {
		state.deleteSavedSpawns();
		rebuild();
	}

	void devSetPreset(ExportPreset preset) {
		state.config.setExportPreset(preset);
		rebuild();
	}

	/** First key from a few candidates that no other binding uses, or -1. */
	int devFreeKey() {
		for (int k : new int[] {org.lwjgl.glfw.GLFW.GLFW_KEY_J, org.lwjgl.glfw.GLFW.GLFW_KEY_K, org.lwjgl.glfw.GLFW.GLFW_KEY_B,
			org.lwjgl.glfw.GLFW.GLFW_KEY_V, org.lwjgl.glfw.GLFW.GLFW_KEY_Y, org.lwjgl.glfw.GLFW.GLFW_KEY_COMMA}) {
			if (conflicts(LandmarkClient.openKey, InputConstants.Type.KEYSYM.getOrCreate(k)).isEmpty()) {
				return k;
			}
		}
		return -1;
	}

	String devConflicts(int glfwKey) {
		var key = InputConstants.Type.KEYSYM.getOrCreate(glfwKey);
		return key.getName() + " conflicts with " + conflicts(LandmarkClient.openKey, key).stream().map(KeyMapping::getName).toList();
	}

	void devScrollTo(double y) {
		list.setScrollAmount(y);
	}

	/** Starts listening on the open-map key and presses {@code glfwKey}, like a player would. */
	void devRebind(int glfwKey) {
		listening = LandmarkClient.openKey;
		keyPressed(new KeyEvent(glfwKey, 0, 0));
	}

	/** Adds explanatory text as rows of up to two lines, wrapped to the list's real width so none of it is cut off. */
	private void note(Component text, int color) {
		var lines = font.split(text, list.getRowWidth() - 6);
		for (int i = 0; i < lines.size(); i += 2) {
			list.add(new NoteRow(lines.subList(i, Math.min(lines.size(), i + 2)), color));
		}
	}

	/** The text, or its start followed by an ellipsis if it does not fit in {@code room} pixels. */
	private String fit(String text, int room) {
		if (font.width(text) <= room) {
			return text;
		}
		return font.plainSubstrByWidth(text, Math.max(0, room - font.width("\u2026"))) + "\u2026";
	}

	private void rebuild() {
		populate();
		list.setScrollAmount(savedScroll);
	}

	private void confirmDeleteSaved() {
		minecraft.setScreenAndShow(new ConfirmScreen(yes -> {
			if (yes) {
				state.deleteSavedSpawns();
			}
			minecraft.setScreenAndShow(this);
			rebuild();
		}, Component.translatable("landmark.config.savedspawns.confirm.title"),
			Component.translatable("landmark.config.savedspawns.confirm.message", state.learned.size())));
	}

	private void confirmResetAll() {
		minecraft.setScreenAndShow(new ConfirmScreen(yes -> {
			if (yes) {
				state.config.resetAllColors();
				Palette.reload(state.config);
			}
			minecraft.setScreenAndShow(this);
		}, Component.translatable("landmark.colors.resetall.title"), Component.translatable("landmark.colors.resetall.message")));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
		g.fill(0, 0, width, height, Palette.get(ColorKey.MAP_BACKGROUND));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		extractBackground(g, mx, my, partial);
		g.centeredText(font, title, width / 2, 12, Palette.get(ColorKey.LIST_OPEN));
		super.extractRenderState(g, mx, my, partial);
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public void removed() {
		state.saveConfig();
		minecraft.options.save();
	}

	// ---- key capture ----

	@Override
	public boolean keyPressed(KeyEvent e) {
		if (listening != null) {
			if (!e.isEscape()) {
				bind(listening, InputConstants.getKey(e));
			}
			listening = null;
			return true;
		}
		return super.keyPressed(e);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		if (listening != null) {
			KeyMapping target = listening;
			listening = null;
			bind(target, InputConstants.Type.MOUSE.getOrCreate(e.button()));
			return true;
		}
		return super.mouseClicked(e, doubleClick);
	}

	private List<KeyMapping> conflicts(KeyMapping target, InputConstants.Key key) {
		List<KeyMapping> out = new ArrayList<>();
		if (key.equals(InputConstants.UNKNOWN)) {
			return out;
		}
		for (KeyMapping other : minecraft.options.keyMappings) {
			if (other != target && !other.isUnbound() && other.matches(key)) {
				out.add(other);
			}
		}
		return out;
	}

	private void apply(KeyMapping target, InputConstants.Key key) {
		target.setKey(key);
		KeyMapping.resetMapping();
		minecraft.options.save();
	}

	private void bind(KeyMapping target, InputConstants.Key key) {
		List<KeyMapping> clash = conflicts(target, key);
		if (clash.isEmpty()) {
			apply(target, key);
			return;
		}
		String names = String.join(", ", clash.stream().map(k -> Component.translatable(k.getName()).getString()).toList());
		minecraft.setScreenAndShow(new ConfirmScreen(yes -> {
			if (yes) {
				apply(target, key);
			}
			minecraft.setScreenAndShow(this);
		}, Component.translatable("landmark.config.conflict.title"),
			Component.translatable("landmark.config.conflict.message", key.getDisplayName(), names)));
	}

	// ---- list ----

	private final class ConfigList extends ContainerObjectSelectionList<Row> {
		ConfigList() {
			super(ConfigScreen.this.minecraft, ConfigScreen.this.width, ConfigScreen.this.height - 32 - 36, 32, ROW_H);
		}

		@Override
		public int getRowWidth() {
			return Math.min(width - 20, 340);
		}

		void add(Row row) {
			addEntry(row);
		}

		void clear() {
			clearEntries();
		}
	}

	private abstract static class Row extends ContainerObjectSelectionList.Entry<Row> {
		final List<GuiEventListener> children = new ArrayList<>();
		final List<NarratableEntry> narratables = new ArrayList<>();

		<T extends GuiEventListener & NarratableEntry> T add(T widget) {
			children.add(widget);
			narratables.add(widget);
			return widget;
		}

		@Override
		public List<? extends GuiEventListener> children() {
			return children;
		}

		@Override
		public List<? extends NarratableEntry> narratables() {
			return narratables;
		}
	}

	private final class HeaderRow extends Row {
		private final Component text;

		HeaderRow(Component text) {
			this.text = text;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			g.text(font, text, getContentX(), getContentBottom() - font.lineHeight - 2, Palette.get(ColorKey.LIST_OPEN), false);
			g.fill(getContentX(), getContentBottom() - 1, getContentRight(), getContentBottom(), Palette.get(ColorKey.PANEL_BORDER));
		}
	}

	private final class SubHeaderRow extends Row {
		private final Component text;

		SubHeaderRow(Component text) {
			this.text = text;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			g.text(font, text, getContentX() + 2, getContentBottom() - font.lineHeight - 2, Palette.get(ColorKey.TEXT_STATUS), false);
		}
	}

	private final class NoteRow extends Row {
		private final List<net.minecraft.util.FormattedCharSequence> lines;
		private final int color;

		NoteRow(List<net.minecraft.util.FormattedCharSequence> lines, int color) {
			this.lines = lines;
			this.color = color;
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			for (int i = 0; i < lines.size(); i++) {
				g.text(font, lines.get(i), getContentX() + 2, getContentY() + 3 + i * (font.lineHeight + 1), color, false);
			}
		}
	}

	private final class KeyRow extends Row {
		private final KeyMapping mapping;
		private final Button keyButton;
		private final Button resetButton;

		KeyRow(KeyMapping mapping) {
			this.mapping = mapping;
			keyButton = add(Button.builder(Component.empty(), b -> listening = mapping).bounds(0, 0, 110, 20).build());
			resetButton = add(Button.builder(Component.translatable("landmark.color.reset"), b -> {
				InputConstants.Key def = mapping.getDefaultKey();
				if (!def.equals(InputConstants.UNKNOWN)) {
					bind(mapping, def);
				}
			}).bounds(0, 0, 50, 20).build());
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			resetButton.setX(getContentRight() - 50);
			resetButton.setY(getContentY() + 2);
			keyButton.setX(resetButton.getX() - 4 - 110);
			keyButton.setY(getContentY() + 2);
			g.text(font, fit(Component.translatable(mapping.getName()).getString(), keyButton.getX() - getContentX() - 8), getContentX() + 2, getContentY() + 7, Palette.get(ColorKey.TEXT_BODY), false);
			boolean waiting = listening == mapping;
			boolean clash = !conflicts(mapping, InputConstants.getKey(mapping.saveString())).isEmpty();
			Component label = mapping.getTranslatedKeyMessage();
			keyButton.setMessage(waiting ? Component.literal("> ").append(label.copy().withStyle(net.minecraft.ChatFormatting.YELLOW)).append(" <")
				: clash ? label.copy().withStyle(net.minecraft.ChatFormatting.RED) : label);
			resetButton.active = !mapping.isDefault();
			keyButton.extractRenderState(g, mx, my, partial);
			resetButton.extractRenderState(g, mx, my, partial);
		}
	}

	/** Shows how many spawn locations are saved, with a button to delete them all. */
	private final class SavedSpawnsRow extends Row {
		private final Button delete;

		SavedSpawnsRow() {
			delete = add(Button.builder(Component.translatable("landmark.config.savedspawns.delete"), b -> confirmDeleteSaved())
				.bounds(0, 0, 70, 20).build());
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			delete.setX(getContentRight() - 70);
			delete.setY(getContentY() + 2);
			delete.active = state.learned.size() > 0;
			String label = Component.translatable("landmark.config.savedspawns", state.learned.size()).getString();
			g.text(font, fit(label, delete.getX() - getContentX() - 8), getContentX() + 2, getContentY() + 7, Palette.get(ColorKey.TEXT_BODY), false);
			delete.extractRenderState(g, mx, my, partial);
		}
	}

	private final class ToggleRow extends Row {
		private final Component label;
		private final CycleButton<Boolean> button;

		ToggleRow(Component label, BooleanSupplier get, Consumer<Boolean> set) {
			this.label = label;
			button = add(CycleButton.onOffBuilder(get.getAsBoolean()).displayOnlyValue().create(0, 0, 70, 20, label, (b, v) -> set.accept(v)));
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			button.setX(getContentRight() - 70);
			button.setY(getContentY() + 2);
			g.text(font, fit(label.getString(), button.getX() - getContentX() - 8), getContentX() + 2, getContentY() + 7, Palette.get(ColorKey.TEXT_BODY), false);
			button.extractRenderState(g, mx, my, partial);
		}
	}

	private final class PresetRow extends Row {
		private final CycleButton<ExportPreset> button;

		PresetRow() {
			button = add(CycleButton.<ExportPreset>builder(p -> Component.translatable(p.translationKey()), state.config.exportPreset())
				.withValues(ExportPreset.values()).displayOnlyValue()
				.create(0, 0, 110, 20, Component.translatable("landmark.config.quality"), (b, v) -> {
					state.config.setExportPreset(v);
					rebuild(); // the description and the warning below depend on the preset
				}));
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			g.text(font, Component.translatable("landmark.config.quality"), getContentX() + 2, getContentY() + 7, Palette.get(ColorKey.TEXT_BODY), false);
			button.setX(getContentRight() - 110);
			button.setY(getContentY() + 2);
			button.extractRenderState(g, mx, my, partial);
		}
	}

	private final class ColorRow extends Row {
		private final ColorKey key;
		private final Button swatch;
		private final EditBox hex;
		private final Button reset;

		ColorRow(ColorKey key) {
			this.key = key;
			swatch = add(Button.builder(Component.empty(), b -> {
				savedScroll = list.scrollAmount();
				minecraft.setScreenAndShow(new ColorPickerScreen(ConfigScreen.this, key));
			}).bounds(0, 0, 26, 20).build());
			hex = add(new EditBox(font, 0, 0, 72, 18, Component.translatable(key.translationKey())));
			hex.setMaxLength(9);
			hex.setValue(ColorMath.format(state.config.color(key), key.hasAlpha));
			hex.setResponder(text -> {
				var parsed = ColorMath.parseHex(text, key.hasAlpha);
				hex.setTextColor(parsed.isPresent() ? 0xFFE0E0E0 : 0xFFFF5555);
				if (parsed.isPresent()) {
					state.config.setColor(key, parsed.getAsInt());
					Palette.reload(state.config);
				}
			});
			reset = add(Button.builder(Component.translatable("landmark.color.reset"), b -> {
				state.config.resetColor(key);
				Palette.reload(state.config);
				hex.setValue(ColorMath.format(key.defaultArgb, key.hasAlpha));
			}).bounds(0, 0, 50, 20).build());
		}

		@Override
		public void extractContent(GuiGraphicsExtractor g, int mx, int my, boolean hovered, float partial) {
			int right = getContentRight();
			reset.setX(right - 50);
			reset.setY(getContentY() + 2);
			hex.setX(reset.getX() - 4 - 72);
			hex.setY(getContentY() + 3);
			swatch.setX(hex.getX() - 4 - 26);
			swatch.setY(getContentY() + 2);
			reset.active = !state.config.isDefaultColor(key);
			int labelRoom = swatch.getX() - getContentX() - 8;
			g.text(font, fit(Component.translatable(key.translationKey()).getString(), labelRoom), getContentX() + 2, getContentY() + 7,
				Palette.get(ColorKey.TEXT_BODY), false);
			swatch.extractRenderState(g, mx, my, partial);
			ColorPickerScreen.swatch(g, swatch.getX() + 4, swatch.getY() + 4, swatch.getWidth() - 8, swatch.getHeight() - 8, state.config.color(key));
			hex.extractRenderState(g, mx, my, partial);
			reset.extractRenderState(g, mx, my, partial);
		}
	}
}
