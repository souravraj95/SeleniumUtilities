
package utility;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementNotVisibleException;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import utility.CustomWait;

public class DriverSupport {

	WebDriver driver;
	MyLogger logger;
	String threadName;

	public DriverSupport(WebDriver driver) {
		this.driver = driver;
		threadName = Thread.currentThread().getName();
		this.logger = new MyLogger(
				Thread.currentThread().getStackTrace()[2].getClassName() + " : " + this.getClass().getSimpleName());
	}

	public DriverSupport() {
		threadName = Thread.currentThread().getName();
		this.logger = new MyLogger(
				Thread.currentThread().getStackTrace()[2].getClassName() + " : " + this.getClass().getSimpleName());
	}

	public void jsId(String id) {
		WebElement element = driver.findElement(By.id(id));
		jsElement(element);
	}

	public void jsXpath(String xpath) {
		WebElement element = driver.findElement(By.xpath(xpath));
		jsElement(element);
	}

	public void jsElement(By byElement) {
		WebElement element = driver.findElement(byElement);
		jsElement(element);
	}

	public void jsElement(WebElement element) {
		((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
	}

	public void jsClick(String locatorType, String locatorValue) {
		WebElement element = driver.findElement(customLocator(locatorType, locatorValue));
		jsElement(element);
	}

	// Switch to Frame without wait
	public boolean switchToFrame(String frameNames) {
		driver.switchTo().defaultContent();
		String[] frames = frameNames.split("~");

		try {
			for (String frame : frames) {
				driver.switchTo().frame(frame);
			}
			logger.debug("Switched to Frame: " + frameNames);
			return true;
		} catch (NoSuchFrameException nfe) {
			return false;
		}
	}

	// Switch to Frame with Wait for the frame to be available
	public void switchToFrameWithWait(String frameNames) {
		driver.switchTo().defaultContent();
		WebDriverWait frameWait = new WebDriverWait(driver, 30);
		String[] frames = frameNames.split("~");

		for (String frameName : frames) {

			try {
				frameWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameName));
				logger.debug("Switched to Frame: " + frameName);

			} catch (JavascriptException | StaleElementReferenceException jse) {
				boolean exitLoop = true;
				int counter = -1;
				do {
					try {
						logger.debug("Frame Switching Failed : " + frameName + " >> Trying Again");
						new CustomWait(driver).staticWait(2);
						frameWait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameName));
						exitLoop = false;
						logger.debug("Switched to Frame: " + frameNames);

					} catch (Exception e) {
					}
				} while (exitLoop && ++counter < 10);
			}
		}
	}

	// Switch to Window with Wait for the window to be available
	public boolean switchToWindow(String windowName) {

		// Return if current window frame is the desired window
		try {
			if (driver.getTitle().equalsIgnoreCase(windowName))
				return true;
		} catch (Exception e) {
		}

		int counter = -1;

		do {
			for (String handle : driver.getWindowHandles()) {
				String windowTitle = driver.switchTo().window(handle).getTitle();
				if (windowTitle.equals(windowName)) {
					driver.switchTo().window(handle);
					logger.debug("Current Window: " + windowName);
					return true;

				} else if (windowName.startsWith("*") && windowTitle.toLowerCase().endsWith(windowName)) {
					driver.switchTo().window(handle);
					logger.debug("Current Window: " + windowTitle);
					return true;

				} else if (windowName.endsWith("*") && windowTitle.toLowerCase().startsWith(windowName)) {
					driver.switchTo().window(handle);
					logger.debug("Current Window: " + windowTitle);
					return true;
				}

			}

			new CustomWait(driver).staticWait(2);
		} while (++counter < 5);

		logger.warning("Window not Found: " + windowName);
		return false;
	}

	public boolean switchPreviousFrameSet() {
		ArrayList<String> frameStack = Environment.ThreadPool.get(threadName).frameStack;
		int lastIndex = frameStack.size();
		if (lastIndex == 0)
			return false;
		String frameSet = frameStack.get(lastIndex - 1);
		frameStack.remove(lastIndex - 1);
		return switchToFrame(frameSet);
	}

	public boolean switchPreviousWindow() {
		ArrayList<String> windowStack = Environment.ThreadPool.get(threadName).windowStack;
		int stackSize = windowStack.size();
		if (stackSize < 2)
			return false;
		String windowName = windowStack.get(stackSize - 2);
		windowStack.remove(stackSize - 1);
		return switchToWindow(windowName);
	}

	public By customLocator(String locatorType, String locatorValue) {
		By byLocator = null;
		switch (locatorType.toLowerCase()) {
		case "name":
			byLocator = By.name(locatorValue);
			break;
		case "id":
			byLocator = By.id(locatorValue);
			break;
		case "cssid":
			if (driver.findElements(By.cssSelector("[id$=" + locatorValue + "]")).size() != 0) {
				byLocator = By.cssSelector("[id$=" + locatorValue + "]");
			} else if (driver.findElements(By.cssSelector("[id^=" + locatorValue + "]")).size() != 0) {
				byLocator = By.cssSelector("[id^=" + locatorValue + "]");
			}
			break;
		case "cssSelector":
			byLocator = By.cssSelector(locatorValue);
			break;
		case "xpath":
			byLocator = By.xpath(locatorValue);
			break;
		case "namecontains":
			byLocator = By.xpath("//*[contains(@name,'" + locatorValue + "')]");
			break;
		case "idcontains":
			byLocator = By.xpath("//*[contains(@id,'" + locatorValue + "')]");
			break;
		case "class":
		case "classname":
			byLocator = By.className(locatorValue);
			break;
		case "linktext":
			byLocator = By.linkText(locatorValue);
			break;
		case "partialLinkText":
			byLocator = By.partialLinkText(locatorValue);
			break;
		case "tagname":
			byLocator = By.tagName(locatorValue);
			break;
		case "NA":
			break;

		default:
			new DesiredException("Locator Type not defined : " + locatorType);
		}
		return byLocator;
	}

	/**
	 * Get Element from the locator
	 * 
	 * @param locatorType
	 * @param locatorValue
	 * @return
	 * @throws ElementNotVisibleException
	 */
	public WebElement getElement(String locatorType, String locatorValue) throws DesiredException {
		By locator = customLocator(locatorType, locatorValue);
		new CustomWait(driver).explicitWait(10, locatorType, locatorValue);

		// Check to be performed
		if (new CustomWait(driver).enableWait(10, locator)) {
			return driver.findElement(locator);
		} else {
			throw new DesiredException("Disable Element: " + locatorType + " -> " + locatorValue);
		}
	}

	public boolean isElement(By locator) {
		try {
			driver.findElement(locator);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isElement(String locatorType, String locatorValue) {
		try {
			driver.findElement(customLocator(locatorType, locatorValue));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public boolean isChildElement(WebElement parentElement, String locatorType, String locatorValue) {
		try {
			parentElement.findElement(customLocator(locatorType, locatorValue));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Return the text of the next td element of the provided element from a table
	 * 
	 * @param fieldElement - Element containing the field Name [td or span]
	 * @return Text of the next td Element
	 */
	public String getFieldValue(WebElement fieldElement) {
		return getElementByReference(fieldElement, 1).getText().trim();
	}

	public String getFieldName(WebElement fieldValueElement) {
		return getElementByReference(fieldValueElement, -1).getText().trim();
	}

	public WebElement getElementByReference(WebElement referenceElement, int referenceIndex) {
		int index;
		WebElement parentElement_tr, parentElement_td;
		List<WebElement> allColumnsInRow;

		// Get <td> for nested element
		parentElement_td = referenceElement;
		while (!parentElement_td.getTagName().equals("td")) {
			parentElement_td = parentElement_td.findElement(By.xpath(".."));
		}

		// Get <tr> for nested element
		parentElement_tr = parentElement_td;
		while (!parentElement_tr.getTagName().equals("tr")) {
			parentElement_tr = parentElement_tr.findElement(By.xpath(".."));
		}

		// Find index of field Element
		allColumnsInRow = parentElement_tr.findElements(By.xpath("td"));
		index = allColumnsInRow.indexOf(parentElement_td);

		return allColumnsInRow.get(index + referenceIndex);
	}

	public String getLabel(WebElement element) {
		String label = "";
		WebElement root = element.findElement(By.xpath(".."));
		List<WebElement> childs = root.findElements(By.xpath(".//*"));
		int index = childs.indexOf(element);
		WebElement previousElement = childs.get(index - 1);
		if (previousElement.getTagName().equals("label")) {
			label = previousElement.getText();
		}
		return label;
	}

	public String getElementLabel(WebElement element) {

		while (true) {
			String label = "Empty_String";
			WebElement previousElement, root;
			root = element.findElement(By.xpath(".."));
			List<WebElement> childs = root.findElements(By.xpath(".//*"));
			int index = childs.indexOf(element);
			for (int i = index; i > 0; i--) {
				previousElement = childs.get(index - 1);
				if (previousElement.getTagName().equals("label")) {
					label = previousElement.getText();
					return label;
				}
			}
			element = root;
		}
	}

	// To be Used for Radio Button & Check-box
	public String getParentLabel(WebElement element) {
		return getElementLabel(element.findElement(By.xpath("..")));
	}

	public Object getIntrinsictProperty(WebElement element) {
		JavascriptExecutor executor = (JavascriptExecutor) driver;
		Object attribute = executor.executeScript("var items = {}; " + "var arg = arguments[0]; "
				+ "for (index = 0; index < arg.attributes.length; ++index) { "
				+ "             items[arg.attributes[index].name] = arg.attributes[index].value " + "}; "
				+ "return items;", element);
		return attribute;
	}

	public Properties getExtrinsicProperty(WebElement element) {
		Properties prop = new Properties();
		// Get Location in Webpage
		int xLocation = element.getLocation().getX();
		int yLocation = element.getLocation().getY();
		double index = Math.sqrt(xLocation * xLocation + yLocation * yLocation);
		System.out.println("Location(x,y->z): " + xLocation + ", " + yLocation + " -> " + index);

		prop.setProperty("xLocation", String.valueOf(xLocation));
		prop.setProperty("yLocation", String.valueOf(yLocation));
		prop.setProperty("index", String.valueOf(index));

		return prop;
	}

	public boolean ScreenShot(String fileName) {
		try {
			File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
			String fileAs = fileName + ".png";
			FileUtils.copyFile(scrFile, new File(fileAs));
			logger.info("Screenshot Saved Successfully: " + fileAs);
			return true;

		} catch (Exception e) {
			logger.warning("Failed to capture Screenshot: " + e.getMessage());
			return false;
		}
	}

	public boolean ScreenShot() {
		long timeStamp = System.currentTimeMillis();
		File reportFolder = new File("Report");
		if (!reportFolder.exists())
			reportFolder.mkdirs();
		String screenShotFileName = "Report\\Screen_" + timeStamp;
		return ScreenShot(screenShotFileName);
	}

	public boolean validateEnvironment() throws DesiredException {
		String pageTitle = driver.getTitle();
		if (pageTitle.contains("This page can’t be displayed") || pageTitle.contains("Forbidden")
				|| pageTitle.contains("Error") || pageTitle.contains("Bad Gateway") || pageTitle.contains("OpenAM")
				|| pageTitle.contains("Not Found")) {
			throw new DesiredException("Environment Unavailable : " + pageTitle);
		}
		return true;
	}

	public void validateLogin() throws DesiredException {
		String pageTitle = driver.getTitle();
		if (pageTitle.contains("attempt to login has been unsuccessful")) {
			throw new DesiredException("Incorrect UserId/Password");

		} else {
			validateEnvironment();
		}
	}
}