
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.awt.image.WritableRaster;

public class SeamCarver {

	public static BufferedImage resize(BufferedImage bufferedImage, int width, int height, boolean debug) {
		int nx = bufferedImage.getWidth() - width;
		int ny = bufferedImage.getHeight() - height;

		// BufferedImage grayedOut = grayOut(bufferedImage);
		BufferedImage energyImage = gradientFilter(bufferedImage);
		BufferedImage enlargeEnergyImg = enlargeEnergy(energyImage);

		double[][] cumulativeEnergyArray = getCumulativeEnergyArray(enlargeEnergyImg);

		BufferedImage removePathImg = bufferedImage;

		if (nx > 0)
			System.out.println("Decreasing width by " + nx);
		if (ny > 0)
			System.out.println("Decreasing height by " + ny);

		for (int n = 0; n < nx; n++) {
			int[] path = findPath(cumulativeEnergyArray);
			cumulativeEnergyArray = removePathEnergyArray(cumulativeEnergyArray, path);
			if (debug) {
				removePathImg = removePathFromImageDebug(removePathImg, path, 0);
			} else {
				removePathImg = removePathFromImage(removePathImg, path);
			}
		}

		cumulativeEnergyArray = rotateArrayCw(cumulativeEnergyArray);
		removePathImg = rotateImageCw(removePathImg);

		for (int n = 0; n < ny; n++) {
			int[] path = findPath(cumulativeEnergyArray);
			cumulativeEnergyArray = removePathEnergyArray(cumulativeEnergyArray, path);
			if (debug) {
				removePathImg = removePathFromImageDebug(removePathImg, path, nx);
			} else {
				removePathImg = removePathFromImage(removePathImg, path);
			}
		}

		cumulativeEnergyArray = rotateArrayCcw(cumulativeEnergyArray);
		removePathImg = rotateImageCcw(removePathImg);

		return removePathImg;
	}

	static double[][] rotateArrayCw(double[][] a) {
		int width = a.length;
		int height = a[0].length;
		double[][] result = new double[height][width];
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				result[j][width - 1 - i] = a[i][j];
			}
		}
		return result;
	}

	static double[][] rotateArrayCcw(double[][] a) {
		int width = a.length;
		int height = a[0].length;
		double[][] result = new double[height][width];
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				result[i][j] = a[j][height - i - 1];
			}
		}
		return result;
	}

	public static BufferedImage rotateImageCw(BufferedImage bufferedImage) {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		BufferedImage result = new BufferedImage(height, width, bufferedImage.getType());
		for (int i = 0; i < height; i++) {
			for (int j = 0; j < width; j++) {
				result.setRGB(i, j, bufferedImage.getRGB(j, height - i - 1));
			}
		}
		return result;
	}

	public static BufferedImage rotateImageCcw(BufferedImage bufferedImage) {
		int width = bufferedImage.getWidth();
		int height = bufferedImage.getHeight();
		BufferedImage result = new BufferedImage(height, width, bufferedImage.getType());
		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				result.setRGB(j, width - 1 - i, bufferedImage.getRGB(i, j));
			}
		}
		return result;
	}

	/*private static BufferedImage grayOut(BufferedImage bufferedImage) {
		ColorConvertOp colorConvert = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
		colorConvert.filter(bufferedImage, bufferedImage);
		return bufferedImage;
	}*/

	private static BufferedImage gradientFilter(BufferedImage img) {
		int type = img.getType();
		int width = img.getWidth();
		int height = img.getHeight();
		BufferedImage temp_img1 = new BufferedImage(width, height, type);
		BufferedImage temp_img2 = new BufferedImage(width, height, type);
		BufferedImage output_img = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);

		float[] matrix_vertical = { -1.0F, 0.0F, 1.0F, -1.0F, 0.0F, 1.0F, -1.0F, 0.0F, 1.0F, };
		float[] matrix_horizontal = { 1.0F, 1.0F, 1.0F, 0.0F, 0.0F, 0.0F, -1.0F, -1.0F, -1.0F, };
		Kernel kernel_v = new Kernel(3, 3, matrix_vertical);
		Kernel kernel_h = new Kernel(3, 3, matrix_horizontal);
		ConvolveOp convolve_v = new ConvolveOp(kernel_v);
		ConvolveOp convolve_h = new ConvolveOp(kernel_h);
		convolve_v.filter(img, temp_img1);
		convolve_h.filter(img, temp_img2);

		WritableRaster raster = output_img.getRaster();

		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				float sum = 0.0f;
				sum = (Math.abs(temp_img1.getRaster().getSample(x, y, 0))
						+ Math.abs(temp_img2.getRaster().getSample(x, y, 0)));
				raster.setSample(x, y, 0, Math.round(sum));
			}
		}
		return output_img;
	}

	private static BufferedImage removePathFromImage(BufferedImage img, int[] path) {
		int type = img.getType();
		int width = img.getWidth();
		int height = img.getHeight();
		int band = 3;
		BufferedImage removePathImg = new BufferedImage(width - 1, height, type);
		WritableRaster raster = removePathImg.getRaster();

		for (int b = 0; b < band; ++b) {
			for (int y = 0; y < height; ++y) {
				for (int x = 0; x <= path[y] - 2; ++x) {
					double temp = 0.0;
					temp = img.getRaster().getSample(x, y, b);
					raster.setSample(x, y, b, Math.round(temp));
				}
				for (int x = path[y] - 1; x < width - 1; ++x) {
					double temp = 0.0;
					temp = img.getRaster().getSample(x + 1, y, b);
					raster.setSample(x, y, b, Math.round(temp));
				}
			}
		}
		return removePathImg;
	}

	private static BufferedImage removePathFromImageDebug(BufferedImage img, int[] path, int nx) {
		int type = img.getType();
		int width = img.getWidth();
		int height = img.getHeight();
		BufferedImage removePathImg = new BufferedImage(width, height, type);

		for (int i = 0; i < width; i++) {
			for (int j = 0; j < height; j++) {
				removePathImg.setRGB(i, j, img.getRGB(i, j));
			}
		}

		for (int y = 0; y < height - nx; y++) {
			removePathImg.setRGB(path[y], y, new Color(255, 0, 0).getRGB());
		}

		return removePathImg;
	}

	private static double[][] removePathEnergyArray(double[][] cumulativeEnergyArray, int[] path) {
		int width = cumulativeEnergyArray[0].length;
		int height = cumulativeEnergyArray.length;
		double[][] new_cumulativeEnergyArray = new double[height][width - 1];
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x <= path[y] - 1; ++x) {
				new_cumulativeEnergyArray[y][x] = cumulativeEnergyArray[y][x];
			}
			for (int x = path[y]; x < width - 1; ++x) {
				new_cumulativeEnergyArray[y][x] = cumulativeEnergyArray[y][x + 1];
			}
		}
		return new_cumulativeEnergyArray;
	}

	private static int getMinIndex(double[] numbers) {
		double minValue = numbers[0];
		int minIndex = 0;
		for (int i = 0; i < numbers.length; i++) {
			if (numbers[i] < minValue) {
				minValue = numbers[i];
				minIndex = i;
			}
		}
		return minIndex;
	}

	private static double getMinValue(double[] numbers) {
		double minValue = numbers[0];
		for (int i = 0; i < numbers.length; i++) {
			if (numbers[i] < minValue) {
				minValue = numbers[i];
			}
		}
		return minValue;
	}

	private static double[][] getCumulativeEnergyArray(BufferedImage img) {
		int width = img.getWidth();
		int height = img.getHeight();
		double[][] cumulative_energy_array = new double[height][width];

		for (int y = 1; y < height; ++y) {
			for (int x = 1; x < width - 1; ++x) {
				cumulative_energy_array[y][x] = (double) img.getRaster().getSample(x, y, 0);
			}
		}

		for (int y = 1; y < height; ++y) {
			for (int x = 1; x < width - 1; ++x) {
				double temp = 0.0;
				double tempArray3[] = new double[3];
				tempArray3[0] = cumulative_energy_array[y - 1][x - 1];
				tempArray3[1] = cumulative_energy_array[y - 1][x];
				tempArray3[2] = cumulative_energy_array[y - 1][x + 1];
				temp = getMinValue(tempArray3) + (double) img.getRaster().getSample(x, y, 0);
				cumulative_energy_array[y][x] = temp;
			}
		}
		return cumulative_energy_array;
	}

	private static int[] findPath(double[][] cumulativeEnergyArray) {
		int width = cumulativeEnergyArray[0].length;
		int height = cumulativeEnergyArray.length;
		int[] path = new int[height];

		double[] tempArray = new double[width - 10];
		int y = height - 1;
		for (int x = 5; x < width - 5; ++x) {
			tempArray[x - 5] = cumulativeEnergyArray[y][x];
		}

		int ind_bot = getMinIndex(tempArray) + 5;
		path[height - 1] = ind_bot;

		int ind_temp = 0;
		double[] tempArray2 = new double[3];
		for (int i = height - 1; i > 0; --i) {
			tempArray2[0] = cumulativeEnergyArray[i - 1][path[i] - 1];
			tempArray2[1] = cumulativeEnergyArray[i - 1][path[i]];
			tempArray2[2] = cumulativeEnergyArray[i - 1][path[i] + 1];
			ind_temp = getMinIndex(tempArray2);
			path[i - 1] = path[i] + ind_temp - 1;
			if (path[i - 1] <= 0) {
				path[i - 1] = 1;
			} else if (path[i - 1] >= width - 1) {
				path[i - 1] = width - 2;
			}
		}
		return path;
	}

	private static BufferedImage enlargeEnergy(BufferedImage img) {
		int type = img.getType();
		int width = img.getWidth();
		int height = img.getHeight();
		BufferedImage enlarge_energy_img = new BufferedImage(width + 2, height, type);
		WritableRaster raster = enlarge_energy_img.getRaster();
		for (int y = 0; y < height; ++y) {
			for (int x = 1; x < width + 1; ++x) {
				raster.setSample(x, y, 0, img.getRaster().getSample(x - 1, y, 0));
			}
		}
		for (int x = 0; x < 10; ++x) {
			for (int y = 0; y < height; ++y) {
				raster.setSample(x, y, 0, 255);
			}
		}
		for (int x = width + 1; x > width - 9; --x) {
			for (int y = 0; y < height; ++y) {
				raster.setSample(x, y, 0, 255);
			}
		}
		return enlarge_energy_img;
	}
}
