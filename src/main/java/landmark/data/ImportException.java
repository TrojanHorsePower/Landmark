package landmark.data;

import java.io.IOException;

/** A user-facing import problem; the message is safe to show in the UI. */
public class ImportException extends IOException {
	public ImportException(String message) {
		super(message);
	}
}
