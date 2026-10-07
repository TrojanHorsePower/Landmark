package landmark.client;

import com.mojang.blaze3d.platform.NativeImage;
import landmark.data.ColorKey;
import landmark.data.ColorMath;
import landmark.LandmarkClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Colour picker: a saturation/brightness square, a hue bar, an opacity bar (for colours that have one) and a hex box. The picker
 * and the hex box always show the same colour: changing either updates the other.
 */
public class ColorPickerScreen extends Screen {
	/** Resolution of the saturation/brightness texture; it is drawn at {@link #sv} pixels, whatever the window size. */
	private static final int TEX = 128;
	private static final int BAR_W = 14;
	private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(LandmarkClient.MOD_ID_FOR_ASSETS, "picker/sv");

	private enum Drag { NONE, AREA, HUE, ALPHA }

	private final Screen parent;
	private final ColorKey key;
	private final LandmarkState state = LandmarkState.get();
	private final int original;
	private float hue;
	private float sat;
	private float val;
	private int alpha;
	/** Side of the picker square on screen: shrinks on short windows so the buttons below never overlap it. */
	private int sv = TEX;
	private Drag drag = Drag.NONE;
	private boolean syncing;
	private boolean hexValid = true;
	private EditBox hexBox;
	private @Nullable DynamicTexture svTexture;
	private float textureHue = -1;

	public ColorPickerScreen(Screen parent, ColorKey key) {
		super(Component.translatable("landmark.picker.title", Component.translatable(key.translationKey())));
		this.parent = parent;
		this.key = key;
		this.original = state.config.color(key);
		setColor(original, null);
	}

	private void setColor(int argb, @Nullable Float keepHue) {
		float[] hsv = ColorMath.toHsv(argb);
		// a grey or black colour has no meaningful hue: keep the one the user was on
		hue = (hsv[1] < 0.001f || hsv[2] < 0.001f) && keepHue != null ? keepHue : hsv[0];
		sat = hsv[1];
		val = hsv[2];
		alpha = key.hasAlpha ? argb >>> 24 : 255;
	}

	private int current() {
		return ColorMath.withAlpha(ColorMath.fromHsv(hue, sat, val), alpha);
	}

	// ---- layout ----

	private int left() {
		return width / 2 - (sv + 10 + BAR_W + (key.hasAlpha ? 10 + BAR_W : 0)) / 2;
	}

	private int top() {
		return 36;
	}

	private int hueX() {
		return left() + sv + 10;
	}

	private int alphaX() {
		return hueX() + BAR_W + 10;
	}

	@Override
	protected void init() {
		sv = Math.max(64, Math.min(TEX, height - 144));
		int rowY = top() + sv + 12;
		hexBox = new EditBox(font, width / 2 - 100, rowY, 90, 18, Component.translatable("landmark.picker.hex"));
		hexBox.setMaxLength(9);
		hexBox.setResponder(this::onHexTyped);
		addRenderableWidget(hexBox);
		addRenderableWidget(Button.builder(Component.translatable("landmark.color.reset"), b -> {
			setColor(key.defaultArgb, hue);
			syncHex();
		}).bounds(width / 2 - 6, rowY, 106, 18).build());
		int by = height - 28;
		addRenderableWidget(Button.builder(Component.translatable("landmark.picker.cancel"), b -> onClose())
			.bounds(width / 2 - 102, by, 100, 20).build());
		addRenderableWidget(Button.builder(Component.translatable("landmark.picker.done"), b -> commit())
			.bounds(width / 2 + 2, by, 100, 20).build());
		syncHex();
	}

	/** Dev harness: types into the hex box. */
	void devHex(String text) {
		hexBox.setValue(text);
	}

	private void syncHex() {
		syncing = true;
		hexBox.setValue(ColorMath.format(current(), key.hasAlpha));
		syncing = false;
		hexValid = true;
		hexBox.setTextColor(0xFFE0E0E0);
	}

	private void onHexTyped(String text) {
		if (syncing) {
			return;
		}
		var parsed = ColorMath.parseHex(text, key.hasAlpha);
		hexValid = parsed.isPresent();
		hexBox.setTextColor(hexValid ? 0xFFE0E0E0 : 0xFFFF5555);
		if (hexValid) {
			setColor(parsed.getAsInt(), hue);
		}
	}

	private void commit() {
		state.config.setColor(key, current());
		Palette.reload(state.config);
		state.saveConfig();
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public void onClose() {
		minecraft.setScreenAndShow(parent);
	}

	@Override
	public void removed() {
		minecraft.getTextureManager().release(TEXTURE_ID);
		svTexture = null;
	}

	// ---- drawing ----

	private void updateTexture() {
		if (svTexture == null) {
			svTexture = new DynamicTexture(() -> "landmark picker", new NativeImage(TEX, TEX, false));
			minecraft.getTextureManager().register(TEXTURE_ID, svTexture);
			textureHue = -1;
		}
		if (textureHue != hue) {
			NativeImage image = svTexture.getPixels();
			for (int y = 0; y < TEX; y++) {
				for (int x = 0; x < TEX; x++) {
					image.setPixel(x, y, ColorMath.fromHsv(hue, x / (TEX - 1f), 1f - y / (TEX - 1f)));
				}
			}
			svTexture.upload();
			textureHue = hue;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float partial) {
		g.fill(0, 0, width, height, Palette.get(ColorKey.MAP_BACKGROUND));
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
		extractBackground(g, mx, my, partial);
		g.centeredText(font, title, width / 2, 14, Palette.get(ColorKey.LIST_OPEN));
		updateTexture();
		int x0 = left();
		int y0 = top();
		g.fill(x0 - 1, y0 - 1, x0 + sv + 1, y0 + sv + 1, 0xFF000000);
		g.blit(svTexture.getTextureView(), svTexture.getSampler(), x0, y0, x0 + sv, y0 + sv, 0f, 1f, 0f, 1f);
		int mx0 = x0 + Math.round(sat * (sv - 1));
		int my0 = y0 + Math.round((1 - val) * (sv - 1));
		g.outline(mx0 - 3, my0 - 3, 7, 7, 0xFF000000);
		g.outline(mx0 - 2, my0 - 2, 5, 5, 0xFFFFFFFF);

		int hx = hueX();
		g.fill(hx - 1, y0 - 1, hx + BAR_W + 1, y0 + sv + 1, 0xFF000000);
		int seg = sv / 6;
		for (int i = 0; i < 6; i++) {
			int yTop = y0 + i * seg;
			int yBottom = i == 5 ? y0 + sv : yTop + seg;
			g.fillGradient(hx, yTop, hx + BAR_W, yBottom, ColorMath.fromHsv(i / 6f, 1, 1), ColorMath.fromHsv((i + 1) / 6f, 1, 1));
		}
		int hy = y0 + Math.round(hue * (sv - 1));
		g.fill(hx - 2, hy - 1, hx + BAR_W + 2, hy + 2, 0xFF000000);
		g.fill(hx - 1, hy, hx + BAR_W + 1, hy + 1, 0xFFFFFFFF);

		if (key.hasAlpha) {
			int ax = alphaX();
			g.fill(ax - 1, y0 - 1, ax + BAR_W + 1, y0 + sv + 1, 0xFF000000);
			checker(g, ax, y0, BAR_W, sv);
			int rgb = ColorMath.fromHsv(hue, sat, val);
			g.fillGradient(ax, y0, ax + BAR_W, y0 + sv, rgb, ColorMath.withAlpha(rgb, 0));
			int ay = y0 + Math.round((1 - alpha / 255f) * (sv - 1));
			g.fill(ax - 2, ay - 1, ax + BAR_W + 2, ay + 2, 0xFF000000);
			g.fill(ax - 1, ay, ax + BAR_W + 1, ay + 1, 0xFFFFFFFF);
		}

		// old and new colour side by side
		int px = x0;
		int py = y0 + sv + 12 + 18 + 22; // below the hex row, leaving room for the labels
		// While the hex code is invalid the message takes the place of the Before/After labels, so it always fits.
		if (hexValid) {
			g.text(font, Component.translatable("landmark.picker.old"), px, py - 10, Palette.get(ColorKey.TEXT_STATUS), false);
			g.text(font, Component.translatable("landmark.picker.new"), px + 70, py - 10, Palette.get(ColorKey.TEXT_STATUS), false);
		} else {
			String msg = Component.translatable("landmark.picker.invalid", key.hasAlpha ? "#RRGGBBAA" : "#RRGGBB").getString();
			g.text(font, font.plainSubstrByWidth(msg, Math.max(40, width - px - 8)), px, py - 10, Palette.get(ColorKey.TEXT_ERROR), false);
		}
		swatch(g, px, py, 60, 18, original);
		swatch(g, px + 70, py, 60, 18, current());
		super.extractRenderState(g, mx, my, partial);
	}

	private static void checker(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		for (int cy = 0; cy < h; cy += 4) {
			for (int cx = 0; cx < w; cx += 4) {
				g.fill(x + cx, y + cy, Math.min(x + w, x + cx + 4), Math.min(y + h, y + cy + 4), ((cx + cy) / 4 & 1) == 0 ? 0xFF808080 : 0xFFC0C0C0);
			}
		}
	}

	static void swatch(GuiGraphicsExtractor g, int x, int y, int w, int h, int argb) {
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
		checker(g, x, y, w, h);
		g.fill(x, y, x + w, y + h, argb);
	}

	// ---- input ----

	@Override
	public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick) {
		if (super.mouseClicked(e, doubleClick)) {
			return true;
		}
		if (e.button() != 0) {
			return false;
		}
		int x0 = left();
		int y0 = top();
		if (e.x() >= x0 && e.x() < x0 + sv && e.y() >= y0 && e.y() < y0 + sv) {
			drag = Drag.AREA;
		} else if (e.x() >= hueX() && e.x() < hueX() + BAR_W && e.y() >= y0 && e.y() < y0 + sv) {
			drag = Drag.HUE;
		} else if (key.hasAlpha && e.x() >= alphaX() && e.x() < alphaX() + BAR_W && e.y() >= y0 && e.y() < y0 + sv) {
			drag = Drag.ALPHA;
		} else {
			return false;
		}
		setFocused(null);
		dragTo(e.x(), e.y());
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) {
		if (drag != Drag.NONE) {
			dragTo(e.x(), e.y());
			return true;
		}
		return super.mouseDragged(e, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent e) {
		drag = Drag.NONE;
		return super.mouseReleased(e);
	}

	private void dragTo(double mx, double my) {
		float fy = (float) Math.max(0, Math.min(1, (my - top()) / (sv - 1)));
		switch (drag) {
			case AREA -> {
				sat = (float) Math.max(0, Math.min(1, (mx - left()) / (sv - 1)));
				val = 1 - fy;
			}
			case HUE -> hue = fy;
			case ALPHA -> alpha = Math.round((1 - fy) * 255);
			default -> {
				return;
			}
		}
		syncHex();
	}
}
