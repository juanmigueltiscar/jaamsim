/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 JaamSim Software Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.input;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpParser.Assigner;
import com.jaamsim.input.ExpParser.EvalContext;
import com.jaamsim.input.ExpParser.OutputResolver;
import com.jaamsim.input.ExpParser.UnitData;
import com.jaamsim.units.AreaUnit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.SpeedUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class TestExpParser {

	private static void assertColSame(double[] vals, ExpResult.Collection col) throws ExpError {
		ExpResult.Iterator colIt = col.getIter();
		for (double val : vals) {
			ExpResult nextKey = colIt.nextKey();
			ExpResult nextVal = col.index(nextKey);
			assertTrue(nextVal.type == ExpResType.NUMBER);
			assertTrue(Math.abs(val - nextVal.value) < 0.00001);
		}
	}

	private static class DummyResolver implements ExpParser.OutputResolver {

		private final String name;
		public DummyResolver(String name) {
			this.name = name;
		}

		@Override
		public ExpResult resolve(EvalContext ec, ExpResult ent)
				throws ExpError {
			if (name.equals("foo")) return ExpResult.makeNumResult(4, DimensionlessUnit.class);
			if (name.equals("bar")) return ExpResult.makeNumResult(3, DimensionlessUnit.class);
			return ExpResult.makeNumResult(1, DimensionlessUnit.class);
		}

		@Override
		public ExpValResult validate(ExpValResult entValRes) {
			return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
		}

	}

	private static class PC extends ExpParser.ParseContext {
		public PC() {
			super(new HashMap<String, ExpResult>());
		}

		@Override
		public UnitData getUnitByName(String name) {
			return null;
		}
		@Override
		public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a,
				Class<? extends Unit> b) {
			return DimensionlessUnit.class;
		}
		@Override
		public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num,
				Class<? extends Unit> denom) {
			return DimensionlessUnit.class;
		}

		@Override
		public OutputResolver getOutputResolver(String name) throws ExpError {
			return new DummyResolver(name);
		}
		@Override
		public OutputResolver getConstOutputResolver(ExpResult constEnt,
				String name) throws ExpError {
			return new DummyResolver(name);
		}
		@Override
		public Assigner getAssigner(String attribName) throws ExpError {
			throw new ExpError(null, 0, "Assign not supported");
		}
		@Override
		public Assigner getConstAssigner(ExpResult constEnt, String attribName)
				throws ExpError {
			throw new ExpError(null, 0, "Assign not supported");
		}
		@Override
		public ExpResult getValFromLitName(String name, String source, int pos) throws ExpError {
			return ExpResult.makeNumResult(1, DimensionlessUnit.class);
		}
	}

	static Entity mapEnt = new Entity();
	static Entity arrayEnt = new Entity();
	static Entity dummyEnt = new Entity();
	static HashMap<Double, Double> map0 = new HashMap<>();
	static HashMap<String, Double> map1 = new HashMap<>();
	static HashMap<String, Double> distanceMap = new HashMap<>();
	static HashMap<Entity, Entity> entMap = new HashMap<>();

	static Entity[] entArray = { dummyEnt };
	static double[] doubleArray = { 1.0, 2.0, 3.0, 42.0 };
	static int[] intArray = { 1, 2, 3, 42 };

	static String[] stringArray = { "foo", "bar" };

	{
		map0.put(1.0, 1.0);
		map0.put(2.0, 42.0);

		map1.put("one", 1.0);
		map1.put("two", 2.0);
		map1.put("everything", 42.0);

		distanceMap.put("near", 1.0);
		distanceMap.put("far", 42000.0);
		distanceMap.put("middling", 4.0);

		entMap.put(dummyEnt, dummyEnt);

	}

	static PC pc = new PC();

	private static class EC extends ExpParser.EvalContext {


	}
	static EC ec = new EC();

	private static void testToken(ExpTokenizer.Token tok, int type, String val) {
		assertTrue(tok.type == type);
		assertTrue(tok.value.equals(val));
	}

	@Test
	public void testTokenize() throws ExpError {
		ArrayList<ExpTokenizer.Token> tokens = ExpTokenizer.tokenize(" a b c 1 2 3 + -");

		assertTrue(tokens.size() == 8);
		testToken(tokens.get(0), ExpTokenizer.VAR_TYPE, "a");
		testToken(tokens.get(1), ExpTokenizer.VAR_TYPE, "b");
		testToken(tokens.get(2), ExpTokenizer.VAR_TYPE, "c");

		testToken(tokens.get(3), ExpTokenizer.NUM_TYPE, "1");
		testToken(tokens.get(4), ExpTokenizer.NUM_TYPE, "2");
		testToken(tokens.get(5), ExpTokenizer.NUM_TYPE, "3");

		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, "+");
		testToken(tokens.get(7), ExpTokenizer.SYM_TYPE, "-");

		tokens = ExpTokenizer.tokenize("foo bar blarg123");
		assertTrue(tokens.size() == 3);
		testToken(tokens.get(0), ExpTokenizer.VAR_TYPE, "foo");
		testToken(tokens.get(1), ExpTokenizer.VAR_TYPE, "bar");
		testToken(tokens.get(2), ExpTokenizer.VAR_TYPE, "blarg123");


		tokens = ExpTokenizer.tokenize("bar.frump ( -12.3)");
		assertTrue(tokens.size() == 7);
		testToken(tokens.get(0), ExpTokenizer.VAR_TYPE, "bar");
		testToken(tokens.get(1), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(2), ExpTokenizer.VAR_TYPE, "frump");
		testToken(tokens.get(3), ExpTokenizer.SYM_TYPE, "(");
		testToken(tokens.get(4), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(5), ExpTokenizer.NUM_TYPE, "12.3");
		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, ")");

		tokens = ExpTokenizer.tokenize("-12.3e6 ... ---");
		assertTrue(tokens.size() == 8);
		testToken(tokens.get(0), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(1), ExpTokenizer.NUM_TYPE, "12.3e6");
		testToken(tokens.get(2), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(3), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(4), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(5), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(7), ExpTokenizer.SYM_TYPE, "-");

		tokens = ExpTokenizer.tokenize("-42.3E-6");
		assertTrue(tokens.size() == 2);
		testToken(tokens.get(0), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(1), ExpTokenizer.NUM_TYPE, "42.3E-6");

		tokens = ExpTokenizer.tokenize("[123][abc]   [+-  2]");
		assertTrue(tokens.size() == 3);
		testToken(tokens.get(0), ExpTokenizer.SQ_TYPE, "123");
		testToken(tokens.get(1), ExpTokenizer.SQ_TYPE, "abc");
		testToken(tokens.get(2), ExpTokenizer.SQ_TYPE, "+-  2");

		// Test long symbol parsing
		tokens = ExpTokenizer.tokenize("&&||==<==&|");
		assertTrue(tokens.size() == 7);
		testToken(tokens.get(0), ExpTokenizer.SYM_TYPE, "&&");
		testToken(tokens.get(1), ExpTokenizer.SYM_TYPE, "||");
		testToken(tokens.get(2), ExpTokenizer.SYM_TYPE, "==");
		testToken(tokens.get(3), ExpTokenizer.SYM_TYPE, "<=");
		testToken(tokens.get(4), ExpTokenizer.SYM_TYPE, "=");
		testToken(tokens.get(5), ExpTokenizer.SYM_TYPE, "&");
		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, "|");

		tokens = ExpTokenizer.tokenize("[[blarg]][foo][[bar]] 42 \"giggity\"");
		assertTrue(tokens.size() == 5);
		testToken(tokens.get(0), ExpTokenizer.STRING_TYPE, "blarg");
		testToken(tokens.get(1), ExpTokenizer.SQ_TYPE, "foo");
		testToken(tokens.get(2), ExpTokenizer.STRING_TYPE, "bar");
		testToken(tokens.get(3), ExpTokenizer.NUM_TYPE, "42");
		testToken(tokens.get(4), ExpTokenizer.STRING_TYPE, "giggity");

	}

	@Test
	public void testParser() throws ExpError {

		ExpParser.Expression exp = ExpParser.parseExpression(pc, "2*5 + 3*5*(3-1)+2");
		double val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "max(3, 42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "min(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == -5);

		exp = ExpParser.parseExpression(pc, "max(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 602);

		exp = ExpParser.parseExpression(pc, "indexOfMin(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 3);

		exp = ExpParser.parseExpression(pc, "indexOfMax(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(pc, "abs(-42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "abs(+42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "[foo].foo*[bar].bar");
		val = exp.evaluate(ec).value;
		assertTrue(val == 12);

		exp = ExpParser.parseExpression(pc, "50/2/5"); // left associative
		val = exp.evaluate(ec).value;
		assertTrue(val == 5);

		exp = ExpParser.parseExpression(pc, "2^2^3"); // right associative
		val = exp.evaluate(ec).value;
		assertTrue(val == 256);

		exp = ExpParser.parseExpression(pc, "1 + 2^2*4 + 2*[foo].foo");
		val = exp.evaluate(ec).value;
		assertTrue(val == 25);

		exp = ExpParser.parseExpression(pc, "1 + 2^(2*4) + 2");
		val = exp.evaluate(ec).value;
		assertTrue(val == 259);

		exp = ExpParser.parseExpression(pc, "2----2"); // A quadruple negative
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(pc, "2---+-2"); // Still a quadruple negative
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(pc, "-1+1");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "(((((1+1)))*5))");
		val = exp.evaluate(ec).value;
		assertTrue(val == 10);

		exp = ExpParser.parseExpression(pc, "!42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "!0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42 == 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42 == 41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42 != 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42 != 41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42 || 0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "0 || 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "0 || 0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42 && 0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "0 && 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "1 && 2");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "!(1&&42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "!!(1&&42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42<41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);
		exp = ExpParser.parseExpression(pc, "41<42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "41>42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42<=41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);
		exp = ExpParser.parseExpression(pc, "41<=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>=41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "41>=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42>=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);
		exp = ExpParser.parseExpression(pc, "42>42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "1==0?42:24");
		val = exp.evaluate(ec).value;
		assertTrue(val == 24);
		exp = ExpParser.parseExpression(pc, "1==1?42:24");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

	}

	private static class TestVariableResolver implements ExpParser.OutputResolver {

		private final String name;
		public TestVariableResolver(String n) {
			this.name = n;
		}

		@Override
		public ExpResult resolve(EvalContext ec, ExpResult ent)
				throws ExpError {
			if (ent.type == ExpResType.ENTITY && ent.entVal == mapEnt) {
				if (name.equals("map0")) {
					return ExpCollections.getCollection(map0, DimensionlessUnit.class);
				}
				if (name.equals("map1")) {
					return ExpCollections.getCollection(map1, DimensionlessUnit.class);
				}
				if (name.equals("entMap")) {
					return ExpCollections.getCollection(entMap, DimensionlessUnit.class);
				}
				if (name.equals("distances")) {
					return ExpCollections.getCollection(distanceMap, DistanceUnit.class);
				}
			}

			if (ent.type == ExpResType.ENTITY && ent.entVal == arrayEnt) {
				if (name.equals("intArray")) {
					return ExpCollections.getCollection(intArray, DimensionlessUnit.class);
				}
				if (name.equals("doubleArray")) {
					return ExpCollections.getCollection(doubleArray, DimensionlessUnit.class);
				}
				if (name.equals("entArray")) {
					return ExpCollections.getCollection(entArray, DimensionlessUnit.class);
				}
				if (name.equals("stringArray")) {
					return ExpCollections.getCollection(stringArray, DimensionlessUnit.class);
				}
			}

			return ExpResult.makeNumResult(1, DimensionlessUnit.class);
		}

		@Override
		public ExpValResult validate(ExpValResult entValRes) {
			return ExpValResult.makeUndecidableRes();
		}
}

	private static class VariableTestPC extends PC {

		@Override
		public OutputResolver getOutputResolver(String name) throws ExpError {
			return new TestVariableResolver(name);
		}
		@Override
		public OutputResolver getConstOutputResolver(ExpResult constEnt,
				String name) throws ExpError {
			return new TestVariableResolver(name);
		}

		@Override
		public ExpResult getValFromLitName(String name, String source, int pos) throws ExpError {
			if (name.equals("Maps")) {
				return ExpResult.makeEntityResult(mapEnt);
			}
			if (name.equals("Arrays")) {
				return ExpResult.makeEntityResult(arrayEnt);
			}
			if (name.equals("Dummy")) {
				return ExpResult.makeEntityResult(dummyEnt);
			}
			return ExpResult.makeNumResult(0, DimensionlessUnit.class);
		}

	}

	@Test
	public void testVariables() throws ExpError {
		VariableTestPC vtpc = new VariableTestPC();

		ExpParser.Expression exp = ExpParser.parseExpression(vtpc, "[Maps].map0(1)");
		double val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(vtpc, "[Maps].map0(2)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(vtpc, "[Maps].map1(\"two\")");
		val = exp.evaluate(ec).value;
		assertTrue(val == 2);

		exp = ExpParser.parseExpression(vtpc, "[Maps].map1(\"everything\")");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(vtpc, "[Maps].entMap([Dummy])");
		ExpResult res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.ENTITY && res.entVal == dummyEnt);

		exp = ExpParser.parseExpression(vtpc, "[Arrays].doubleArray(4)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(vtpc, "maxCol([Arrays].doubleArray)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(vtpc, "indexOfMaxCol([Arrays].doubleArray)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(vtpc, "minCol([Arrays].intArray)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(vtpc, "indexOfMinCol([Arrays].intArray)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(vtpc, "indexOf([Arrays].intArray, 42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(vtpc, "indexOf([Maps].map1, 42)");
		res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.STRING);
		assertTrue(res.stringVal.equals("everything"));

		exp = ExpParser.parseExpression(vtpc, "[Arrays].intArray(4)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(vtpc, "[Arrays].entArray(1)");
		res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.ENTITY && res.entVal == dummyEnt);

		exp = ExpParser.parseExpression(vtpc, "[Arrays].stringArray(2)");
		res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.STRING && res.stringVal.equals("bar"));

		exp = ExpParser.parseExpression(vtpc, "maxCol([Maps].distances)");
		res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.NUMBER);
		assertTrue(res.unitType == DistanceUnit.class);
		assertTrue(res.value == 42000.0);

		exp = ExpParser.parseExpression(vtpc, "indexOfMaxCol([Maps].distances)");
		res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.STRING);
		assertTrue(res.stringVal.equals("far"));

		exp = ExpParser.parseExpression(vtpc, "size([Arrays].doubleArray)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

	}
	@Test
	public void testArray() throws ExpError {
		ExpParser.Expression exp = ExpParser.parseExpression(pc, "{1, 2, 3, 4}(2)");
		ExpResult res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.NUMBER);
		assertTrue(res.value == 2);

		exp = ExpParser.parseExpression(pc, "{[[foo]], [[bar]], \"baz\"}(3)");
		res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.STRING);
		assertTrue(res.stringVal.equals("baz"));

	}

	@Test
	public void testString() throws ExpError {
		ExpParser.Expression exp = ExpParser.parseExpression(pc, "[[stringly]]");
		ExpResult res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.STRING);
		assertTrue(res.stringVal.equals("stringly"));

		exp = ExpParser.parseExpression(pc, "[[stri]] + \"ngly\"");
		res = exp.evaluate(ec);
		assertTrue(res.type == ExpResType.STRING);
		assertTrue(res.stringVal.equals("stringly"));
	}

	@Test
	public void testLambda() throws ExpError {

		ExpParser.Expression exp = ExpParser.parseExpression(pc, "|x|(2*x)");
		ExpResult val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.LAMBDA);

		exp = ExpParser.parseExpression(pc, "|x|(2*x)(21)");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 42);

		exp = ExpParser.parseExpression(pc, "|x,y|(|z|(x*y*z)(2))(3,7)");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 42);

		exp = ExpParser.parseExpression(pc, "|x,y|(x*y)(2,21)");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 42);

		exp = ExpParser.parseExpression(pc, "map(|x|(x*2), {1, 2, 3, 21})");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);
		double[] mapVals = {2, 4, 6, 42};
		assertColSame(mapVals, val.colVal);

		exp = ExpParser.parseExpression(pc, "map(|x, key|(key*2), {1, 2, 3, 21})");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);
		double[] mapVals2 = {2, 4, 6, 8};
		assertColSame(mapVals2, val.colVal);

		exp = ExpParser.parseExpression(pc, "filter(|x|(x>20), {1, 2, 3, 21, 5, 42})");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);
		double[] filterVals = {21, 42};
		assertColSame(filterVals, val.colVal);

		exp = ExpParser.parseExpression(pc, "filter(|x|(x>20), map(|x|(x*2), {1, 2, 3, 11, 5, 21}))");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);
		double[] mapFilterVals = {22, 42};
		assertColSame(mapFilterVals, val.colVal);

		exp = ExpParser.parseExpression(pc, "reduce(|val, accum|(val + accum), 0, {1,2,3,4,32})");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 42);

		exp = ExpParser.parseExpression(pc, "reduce(|val, accum|(val * accum), 1, {1,2,3,4,5})");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 120);

		exp = ExpParser.parseExpression(pc, "sort( |x, y|(x<y), {5,2,3,42,4,1} )");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);
		double[] sortVals = {1, 2, 3, 4, 5, 42};
		assertColSame(sortVals, val.colVal);

	}

	@Test
	public void testLocalVars() throws ExpError {
		ExpParser.Expression exp = ExpParser.parseExpression(pc, "x = 2; y = x*3; z = 7; y*z");
		ExpResult val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 42);

		exp = ExpParser.parseExpression(pc, "array = {1,2,4,21}; mapped = map(|x|(x*2), array); filter(|x|(x>20), mapped)");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);
		double[] vals = {42};
		assertColSame(vals, val.colVal);
	}

	@Test
	public void testRecursion() throws ExpError {
		String expStr = "" ;
		expStr += "recFact = |val, rec| ( (val < 1) ? 1 : val * rec(val-1, rec) );";
		expStr += "fact = |val|( recFact(val, recFact) );";
		expStr += "fact(6)";
		ExpParser.Expression exp = ExpParser.parseExpression(pc, expStr);
		ExpResult val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 720);

		expStr = "" ;
		expStr += "thresh = 3;";
		expStr += "num = 12;";
		expStr += "recFib = |val, rf| ( (val < thresh) ? 1 : (rf(val-2, rf) + rf(val-1, rf)) );";
		expStr += "fib = |val| (recFib(val, recFib) );";
		expStr += "fib(num)";
		exp = ExpParser.parseExpression(pc, expStr);
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.NUMBER);
		assertTrue(val.value == 144);

	}

	@Test
	public void testRange() throws ExpError {

		ExpParser.Expression exp = ExpParser.parseExpression(pc, "range(5)");
		ExpResult val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);

		double[] vals0 = {1, 2, 3, 4, 5};
		assertColSame(vals0, val.colVal);

		exp = ExpParser.parseExpression(pc, "range(10, 15)");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);

		double[] vals1 = {10, 11, 12, 13, 14, 15};
		assertColSame(vals1, val.colVal);

		exp = ExpParser.parseExpression(pc, "range(1, 2, 0.2)");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);

		double[] vals2 = {1.0, 1.2, 1.4, 1.6, 1.8, 2.0};
		assertColSame(vals2, val.colVal);

		exp = ExpParser.parseExpression(pc, "range(2, 1)");
		val = exp.evaluate(ec);
		assertTrue(val.type == ExpResType.COLLECTION);

		double[] vals3 = { };
		assertColSame(vals3, val.colVal);

	}

	@Test
	public void testUnits() throws ExpError {
		class ErrorResolver implements ExpParser.OutputResolver {

			private final ExpError error = new ExpError(null, 0, "Variables not supported in test");

			@Override
			public ExpResult resolve(EvalContext ec, ExpResult ent) throws ExpError {
				throw error;
			}

			@Override
			public ExpValResult validate(ExpValResult entValRes) {
				return ExpValResult.makeErrorRes(error);
			}
		}

		class UnitPC extends ExpParser.ParseContext {
			public UnitPC() {
				super(new HashMap<String, ExpResult>());
			}

			@Override
			public UnitData getUnitByName(String name) {
				UnitData ret = new UnitData();
				if (name.equals("s")) {
					ret.scaleFactor = 1;
					ret.unitType = TimeUnit.class;
					return ret;
				}
				if (name.equals("min")) {
					ret.scaleFactor = 60;
					ret.unitType = TimeUnit.class;
					return ret;
				}
				if (name.equals("hr")) {
					ret.scaleFactor = 3600;
					ret.unitType = TimeUnit.class;
					return ret;
				}
				if (name.equals("m")) {
					ret.scaleFactor = 1;
					ret.unitType = DistanceUnit.class;
					return ret;
				}
				if (name.equals("km")) {
					ret.scaleFactor = 1000;
					ret.unitType = DistanceUnit.class;
					return ret;
				}
				return null;
			}
			@Override
			public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a,
					Class<? extends Unit> b) {
				return Unit.getMultUnitType(a, b);
			}

			@Override
			public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num,
					Class<? extends Unit> denom) {
				return Unit.getDivUnitType(num, denom);
			}
			@Override
			public OutputResolver getOutputResolver(String name)
					throws ExpError {
				return new ErrorResolver();
			}
			@Override
			public Assigner getAssigner(String attribName) throws ExpError {
				throw new ExpError(null, 0, "Assign not supported");
			}
			@Override
			public Assigner getConstAssigner(ExpResult constEnt, String attribName)
					throws ExpError {
				throw new ExpError(null, 0, "Assign not supported");
			}
			@Override
			public ExpResult getValFromLitName(String name, String source, int pos) throws ExpError {
				return null;
			}
			@Override
			public OutputResolver getConstOutputResolver(ExpResult constEnt,
					String name) throws ExpError {
				return new ErrorResolver();
			}
			@Override
			public boolean isVarName(String varName) {
				return false;
			}
			@Override
			public boolean isVarConstant(String varName) {
				return false;
			}
			@Override
			public ExpResult getValFromConstVar(String name, String source, int pos) throws ExpError {
				return null;
			}

		}
		UnitPC upc = new UnitPC();

		ExpParser.Expression exp = ExpParser.parseExpression(upc, "1[km] + 1[m]");
		ExpResult res = exp.evaluate(ec);
		assertTrue(res.value == 1001);
		assertTrue(res.unitType == DistanceUnit.class);

		exp = ExpParser.parseExpression(upc, "1[hr] + 1[min] + 5[s]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 3665);
		assertTrue(res.unitType == TimeUnit.class);

		boolean threw = false;
		try {
			exp = ExpParser.parseExpression(upc, "1[hr] + 1[m]");
			res = exp.evaluate(ec);
			assertTrue(false);
		} catch (ExpError ex) {
			threw = true;
		}
		assertTrue(threw);

		exp = ExpParser.parseExpression(upc, "max(1[hr], 1[s])");
		res = exp.evaluate(ec);
		assertTrue(res.value == 3600);
		assertTrue(res.unitType == TimeUnit.class);

		exp = ExpParser.parseExpression(upc, "6*1[km]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 6000);
		assertTrue(res.unitType == DistanceUnit.class);

		threw = false;
		try {
			exp = ExpParser.parseExpression(upc, "1[parsec]");
		} catch(ExpError ex) {
			threw = true;
		}
		assertTrue(threw);

		exp = ExpParser.parseExpression(upc, "1[m]/1[s]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 1);
		assertTrue(res.unitType == SpeedUnit.class);

		exp = ExpParser.parseExpression(upc, "5[m]*1[km]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 5000);
		assertTrue(res.unitType == AreaUnit.class);

		exp = ExpParser.parseExpression(upc, "5[m]/1[m]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 5);
		assertTrue(res.unitType == DimensionlessUnit.class);

	}

	private static class AssignContainer {
		public ExpResult res;
		public ExpResult.Collection col;
		public String lastAttribName;
	}

	private static class TestAssigner implements ExpParser.Assigner {

		AssignContainer cont;
		String attribName;

		TestAssigner(AssignContainer cont, String attribName) {
			this.cont = cont;
			this.attribName = attribName;
		}
		@Override
		public void assign(ExpResult ent, ExpResult index, ExpResult val)
				throws ExpError {
			if (index == null) {
				cont.res = val;
			} else {
				cont.col = cont.col.assign(index, val);
			}
			cont.lastAttribName = attribName;
		}
	}

	private static class AssignPC extends PC {
		AssignContainer cont;

		AssignPC(AssignContainer cont) {
			this.cont = cont;
		}

		@Override
		public Assigner getAssigner(String attribName) throws ExpError {
			return new TestAssigner(cont, attribName);
		}
		@Override
		public Assigner getConstAssigner(ExpResult constEnt, String attribName)
				throws ExpError {
			return new TestAssigner(cont, attribName);
		}

	}

	@Test
	public void testAssignment() throws ExpError {
		AssignContainer cont = new AssignContainer();

		ArrayList<ExpResult> initialRes = new ArrayList<>();
		initialRes.add(ExpResult.makeNumResult(42, DimensionlessUnit.class));
		cont.col = ExpCollections.makeExpressionCollection(initialRes, false).colVal;
		AssignPC apc = new AssignPC(cont);

		ExpParser.Assignment assign = ExpParser.parseAssignment(apc, "[foo].arg = 40 + 2");
		ExpResult res = assign.evaluate(ec);
		assert(res.value == 42.0);
		assert(cont.res.type == ExpResType.NUMBER);
		assert(cont.res.value == 42.0);
		assert(cont.lastAttribName.equals("arg"));

		assign = ExpParser.parseAssignment(apc, "[foo].blarg(42) = 40 + 5");
		res = assign.evaluate(ec);
		ExpResult contained = cont.col.index(ExpResult.makeNumResult(42, DimensionlessUnit.class));
		assert(res.value == 45.0);
		assert(contained.type == ExpResType.NUMBER);
		assert(contained.value == 45.0);
		assert(cont.lastAttribName.equals("blarg"));
	}
}
