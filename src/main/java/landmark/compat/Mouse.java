package landmark.compat;

/** A mouse press, drag or release, in GUI coordinates; the same shape in every Minecraft version. */
public record Mouse(double x, double y, int button) {}
