package com.instantiations.installer;

import com.instantiations.eclipse.shared.installer.BaseProductInstaller;
import com.instantiations.eclipse.shared.installer.SubProduct;
import com.instantiations.installer.core.steps.ChooseEclipseStep;

/**
 * A generic product installer. 
 * This is a Groovy template, so it has Groovy variables that are replaced at build time.
 */

public class ProductInstaller extends BaseProductInstaller
{
	private static final SubProduct PRIMARY_PRODUCT = new com.instantiations.subproducts.${product_name}SubProduct();

	/**
	 * Main installer entry point. Supported command line arguments can be found as
	 * constants in {@link com.instantiations.installer.core.InstallOptions}
	 * 
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		try {
			new ProductInstaller().run(args);
		}
		catch (Exception e) {
			showException(e);
		}
	}

	/**
	 * Filter the list of Eclipse installations generated by the superclass
	 */
	protected ChooseEclipseStep chooseEclipseStep() {
		ChooseEclipseStep step = super.chooseEclipseStep();
		step.setCompatibility("[${installer_target_versions_low}.0,${installer_target_versions_high}.0)");
		return step;
	}

	protected SubProduct getPrimaryProduct() {
		return PRIMARY_PRODUCT;
	}

	protected SubProduct[] getSubProductsToInstall() {
		return new SubProduct[]{
			${all_subproducts}
		};
	}
}
