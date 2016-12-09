/**
 *
 */
package es.um.nosql.schemainference.json2dbschema.intermediate.firsto;

import com.google.common.collect.Range;

import es.um.nosql.schemainference.json2dbschema.intermediate.raw.NumberSC;

/**
 * @author dsevilla
 *
 */
public class NumberWithRangeSC extends NumberSC implements Ranged
{
	private Range<Double> range;

	/* (non-Javadoc)
	 * @see es.um.nosql.JSONSchema.Ranged#getRange()
	 */
	@Override
	public Range<Double> getRange() {
		return range;
	}

	/* (non-Javadoc)
	 * @see es.um.nosql.JSONSchema.Ranged#setRange(com.google.common.collect.Range)
	 */
	public void setRange(Range<Double> range) {
		this.range = range;
	}
}