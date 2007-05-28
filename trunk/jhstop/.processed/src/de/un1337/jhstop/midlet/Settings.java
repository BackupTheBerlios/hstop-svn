package de.un1337.jhstop.midlet;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.TextField;

import de.un1337.jhstop.items.SaveChoiceGroup;
import de.un1337.jhstop.items.SaveTextField;

public class Settings extends SettingForm {
	private static String NAME_AGENT = "agent";

	private static String NAME_USER = "user";

	private static String NAME_PWD = "pwd";

	private static String NAME_URL = "url";

	// private static String NAME_PROXY = "proxy";

	private static String NAME_VERIFY = "verify";

	private SaveTextField fieldAgent;

	private SaveTextField fieldUser;

	private SaveTextField fieldPwd;

	private SaveTextField fieldUrl;

	// private SaveTextField fieldProxy;

	private SaveChoiceGroup choiceVerify;

	public Settings(Display d, Form mainform, jhstopc listener, String name) {
		super(d, mainform, listener, name, jhstopc.NAME_RS_SETTINGS);

		fieldAgent = new SaveTextField("agent", "", 50, TextField.ANY, NAME_AGENT);
		fieldUser = new SaveTextField("user", "", 25, TextField.ANY, NAME_USER);
		fieldPwd = new SaveTextField("password", "", 25, TextField.PASSWORD, NAME_PWD);
		fieldUrl = new SaveTextField("url", "", 128, TextField.ANY, NAME_URL);
		// fieldProxy = new SaveTextField("proxy", "", 128, TextField.ANY,
		// NAME_PROXY);
		choiceVerify = new SaveChoiceGroup("verify host", Choice.POPUP, NAME_VERIFY);

		choiceVerify.append("yes", null);
		choiceVerify.append("no", null);

		fieldAgent.setDefaultCommand(jhstopc.cmdOK);
		fieldAgent.setItemCommandListener(this);
		fieldUser.setDefaultCommand(jhstopc.cmdOK);
		fieldUser.setItemCommandListener(this);
		fieldPwd.setDefaultCommand(jhstopc.cmdOK);
		fieldPwd.setItemCommandListener(this);
		// fieldProxy.setDefaultCommand(jhstopc.cmdOK);
		// fieldProxy.setItemCommandListener(this);
		fieldUrl.setDefaultCommand(jhstopc.cmdOK);
		fieldUrl.setItemCommandListener(this);
		choiceVerify.setDefaultCommand(jhstopc.cmdOK);
		choiceVerify.setItemCommandListener(this);

		form.append(fieldAgent);
		form.append(fieldUser);
		form.append(fieldPwd);
		form.append(fieldUrl);
		// form.append(fieldProxy);
		form.append(choiceVerify);

		loadFromFile();

	}

	public String getAgent() {
		return fieldAgent.getData();
	}

	public String getUser() {
		return fieldUser.getData();
	}

	public String getPwd() {
		return fieldPwd.getData();
	}

	public String getURL() {
		return fieldUrl.getData();
	}

	/*
	 * public String getProxy() { return fieldProxy.getData(); }
	 */

	public boolean getVerifySSL() {
		return choiceVerify.getData().compareTo("yes") == 0;
	}

}
