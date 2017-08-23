package org.prettycat.dataflow.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.Interpreter;
import org.objectweb.asm.tree.analysis.Value;

public class SimpleFlowInterpreter extends Interpreter implements Opcodes {
	
	private HashMap<AbstractInsnNode, SimpleFlowValue> values;

    public SimpleFlowInterpreter() {
        super(ASM5);
        values = new HashMap<AbstractInsnNode, SimpleFlowValue>();
    }

    @Override
    public Value newValue(final Type type) {
    	return newValue(type, null);
    }
    
    public Value newValue(final Type type, final AbstractInsnNode origin) {
    	return newValue(type, origin, (Collection<SimpleFlowValue>)null);
    }

    public Value newValue(final Type type, final AbstractInsnNode origin, final Collection<SimpleFlowValue> inputs) {
    	if (type != null && type.equals(Type.VOID_TYPE) && origin == null && (inputs == null || inputs.isEmpty())) {
    		return null;
    	}
    	SimpleFlowValue result = new SimpleFlowValue(type, origin, inputs, false);
    	if (origin != null) {
    		values.put(origin, result);
    	}
    	return result;
    }

    public Value newValue(
    		final Type type, 
    		final AbstractInsnNode origin,
    		SimpleFlowValue... inputs) 
    {
    	List<SimpleFlowValue> items = Arrays.asList(inputs).stream().filter(
    			value -> value != null
    			).collect(Collectors.toList());
    	return newValue(type, origin, items);
    }

    @Override
    public Value newOperation(final AbstractInsnNode insn) throws AnalyzerException {
        switch (insn.getOpcode()) {
        case ACONST_NULL:
            return newValue(Type.getObjectType("null"), insn);
        case ICONST_M1:
        case ICONST_0:
        case ICONST_1:
        case ICONST_2:
        case ICONST_3:
        case ICONST_4:
        case ICONST_5:
            return newValue(Type.INT_TYPE, insn);
        case LCONST_0:
        case LCONST_1:
            return newValue(Type.LONG_TYPE, insn);
        case FCONST_0:
        case FCONST_1:
        case FCONST_2:
            return newValue(Type.FLOAT_TYPE, insn);
        case DCONST_0:
        case DCONST_1:
            return newValue(Type.DOUBLE_TYPE, insn);
        case BIPUSH:
        case SIPUSH:
            return newValue(Type.INT_TYPE, insn);
        case LDC:
            Object cst = ((LdcInsnNode) insn).cst;
            if (cst instanceof Integer) {
                return newValue(Type.INT_TYPE, insn);
            } else if (cst instanceof Float) {
                return newValue(Type.FLOAT_TYPE, insn);
            } else if (cst instanceof Long) {
                return newValue(Type.LONG_TYPE, insn);
            } else if (cst instanceof Double) {
                return newValue(Type.DOUBLE_TYPE, insn);
            } else if (cst instanceof String) {
                return newValue(Type.getObjectType("java/lang/String"), insn);
            } else if (cst instanceof Type) {
                int sort = ((Type) cst).getSort();
                if (sort == Type.OBJECT || sort == Type.ARRAY) {
                    return newValue(Type.getObjectType("java/lang/Class"), insn);
                } else if (sort == Type.METHOD) {
                    return newValue(Type
                            .getObjectType("java/lang/invoke/MethodType"), insn);
                } else {
                    throw new IllegalArgumentException("Illegal LDC constant "
                            + cst);
                }
            } else if (cst instanceof Handle) {
                return newValue(Type
                        .getObjectType("java/lang/invoke/MethodHandle"), insn);
            } else {
                throw new IllegalArgumentException("Illegal LDC constant "
                        + cst);
            }
        case JSR:
            return SimpleFlowValue.newReturnAddressValue();
        case GETSTATIC:
            return newValue(Type.getType(((FieldInsnNode) insn).desc), insn);
        case NEW:
            return newValue(Type.getObjectType(((TypeInsnNode) insn).desc), insn);
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public Value copyOperation(final AbstractInsnNode insn,
            final Value value) throws AnalyzerException {
    	// SimpleFlowValue sv = (SimpleFlowValue)value;
        // return new SimpleFlowValue(sv.type, insn, Arrays.asList(new SimpleFlowValue[]{sv}), true);
    	return value;
    }

    @Override
    public Value unaryOperation(final AbstractInsnNode insn,
            final Value value) throws AnalyzerException {
        switch (insn.getOpcode()) {
        case INEG:
        case IINC:
        case L2I:
        case F2I:
        case D2I:
        case I2B:
        case I2C:
        case I2S:
            return newValue(Type.INT_TYPE, insn, (SimpleFlowValue)value);
        case FNEG:
        case I2F:
        case L2F:
        case D2F:
            return newValue(Type.FLOAT_TYPE, insn, (SimpleFlowValue)value);
        case LNEG:
        case I2L:
        case F2L:
        case D2L:
            return newValue(Type.LONG_TYPE, insn, (SimpleFlowValue)value);
        case DNEG:
        case I2D:
        case L2D:
        case F2D:
            return newValue(Type.DOUBLE_TYPE, insn, (SimpleFlowValue)value);
        case IFEQ:
        case IFNE:
        case IFLT:
        case IFGE:
        case IFGT:
        case IFLE:
        case TABLESWITCH:
        case LOOKUPSWITCH:
        case IRETURN:
        case LRETURN:
        case FRETURN:
        case DRETURN:
        case ARETURN:
        case PUTSTATIC:
        	newValue(null, insn, (SimpleFlowValue)value);
            return null;
        case GETFIELD:
            return newValue(Type.getType(((FieldInsnNode) insn).desc), insn, (SimpleFlowValue)value);
        case NEWARRAY:
            switch (((IntInsnNode) insn).operand) {
            case T_BOOLEAN:
                return newValue(Type.getType("[Z"), insn, (SimpleFlowValue)value);
            case T_CHAR:
                return newValue(Type.getType("[C"), insn, (SimpleFlowValue)value);
            case T_BYTE:
                return newValue(Type.getType("[B"), insn, (SimpleFlowValue)value);
            case T_SHORT:
                return newValue(Type.getType("[S"), insn, (SimpleFlowValue)value);
            case T_INT:
                return newValue(Type.getType("[I"), insn, (SimpleFlowValue)value);
            case T_FLOAT:
                return newValue(Type.getType("[F"), insn, (SimpleFlowValue)value);
            case T_DOUBLE:
                return newValue(Type.getType("[D"), insn, (SimpleFlowValue)value);
            case T_LONG:
                return newValue(Type.getType("[J"), insn, (SimpleFlowValue)value);
            default:
                throw new AnalyzerException(insn, "Invalid array type");
            }
        case ANEWARRAY:
            String desc = ((TypeInsnNode) insn).desc;
            return newValue(Type.getType("[" + Type.getObjectType(desc)), insn, (SimpleFlowValue)value);
        case ARRAYLENGTH:
            return newValue(Type.INT_TYPE, insn, (SimpleFlowValue)value);
        case ATHROW:
        	newValue(null, insn, (SimpleFlowValue)value);
            return null;
        case CHECKCAST:
            desc = ((TypeInsnNode) insn).desc;
            return newValue(Type.getObjectType(desc), insn, (SimpleFlowValue)value);
        case INSTANCEOF:
            return newValue(Type.INT_TYPE, insn, (SimpleFlowValue)value);
        case MONITORENTER:
        case MONITOREXIT:
        case IFNULL:
        case IFNONNULL:
        	newValue(null, insn, (SimpleFlowValue)value);
            return null;
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public Value binaryOperation(
    		final AbstractInsnNode insn,
            final Value value1, 
            final Value value2)
            throws AnalyzerException {
        switch (insn.getOpcode()) {
        case IALOAD:
        case BALOAD:
        case CALOAD:
        case SALOAD:
        case IADD:
        case ISUB:
        case IMUL:
        case IDIV:
        case IREM:
        case ISHL:
        case ISHR:
        case IUSHR:
        case IAND:
        case IOR:
        case IXOR:
            return newValue(Type.INT_TYPE, insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2);
        case FALOAD:
        case FADD:
        case FSUB:
        case FMUL:
        case FDIV:
        case FREM:
            return newValue(Type.FLOAT_TYPE, insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2);
        case LALOAD:
        case LADD:
        case LSUB:
        case LMUL:
        case LDIV:
        case LREM:
        case LSHL:
        case LSHR:
        case LUSHR:
        case LAND:
        case LOR:
        case LXOR:
            return newValue(Type.LONG_TYPE, insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2);
        case DALOAD:
        case DADD:
        case DSUB:
        case DMUL:
        case DDIV:
        case DREM:
            return newValue(Type.DOUBLE_TYPE, insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2);
        case AALOAD:
            return newValue(Type.getObjectType("java/lang/Object"), insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2);
        case LCMP:
        case FCMPL:
        case FCMPG:
        case DCMPL:
        case DCMPG:
            return newValue(Type.INT_TYPE, insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2);
        case IF_ICMPEQ:
        case IF_ICMPNE:
        case IF_ICMPLT:
        case IF_ICMPGE:
        case IF_ICMPGT:
        case IF_ICMPLE:
        case IF_ACMPEQ:
        case IF_ACMPNE:
        case PUTFIELD:
        	newValue(null, insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2);
            return null;
        default:
            throw new Error("Internal error.");
        }
    }

    @Override
    public Value ternaryOperation(final AbstractInsnNode insn,
            final Value value1, 
            final Value value2,
            final Value value3) throws AnalyzerException 
    {
    	return newValue(null, insn, (SimpleFlowValue)value1, (SimpleFlowValue)value2, (SimpleFlowValue)value3);
    }

    @Override
    public Value naryOperation(
    		final AbstractInsnNode insn,
			final List values) throws AnalyzerException {
        int opcode = insn.getOpcode();
        if (opcode == MULTIANEWARRAY) {
            return newValue(Type.getType(((MultiANewArrayInsnNode) insn).desc), insn, (List<SimpleFlowValue>)values);
        } else if (opcode == INVOKEDYNAMIC) {
            return newValue(Type
                    .getReturnType(((InvokeDynamicInsnNode) insn).desc), insn, (List<SimpleFlowValue>)values);
        } else {
            return newValue(Type.getReturnType(((MethodInsnNode) insn).desc), insn, (List<SimpleFlowValue>)values);
        }
    }

    @Override
    public void returnOperation(
    		final AbstractInsnNode insn,
            final Value value, 
            final Value expected)
            throws AnalyzerException {
    	newValue(null, insn, (SimpleFlowValue)value);
    }

    @Override
    public Value merge(final Value v, final Value w) {
    	// we need a more complex merge implementation, especially for parameters.
		final SimpleFlowValue sv = (SimpleFlowValue)v;
    	final SimpleFlowValue sw = (SimpleFlowValue)w;
    	
    	// System.out.println("merge(" + sv + ", " + sw + ")");
    	
    	/*if (sw.origin == sv.origin && sw.type == sv.type) {
    		return sv;
    	}
    	
    	Type type = sv.type;
    	
    	
    	if (sv.isMerge) {
        	ArrayList<SimpleFlowValue> inputs = new ArrayList<>();
        	inputs.addAll(sv.inputs);
        	
        	if (sw.isMerge) {
        		for (SimpleFlowValue input: sw.inputs) {
        			if (inputs.contains(input)) {
        				continue;
        			}
        			inputs.add(input);
        		}
        	} else {
        		inputs.add(sw);
        	}
        	
        	for (SimpleFlowValue input: inputs) {
        		if (type == null || input.type == null || !input.type.equals(type)) {
        			type = null;
        			break;
        		}
        	}
        	
        	return new SimpleFlowValue(type, null, inputs, true);
    	} else if (sw.isMerge) {
        	ArrayList<SimpleFlowValue> inputs = new ArrayList<>();
        	inputs.add(sv);
        	inputs.addAll(sw.inputs);
        	
        	for (SimpleFlowValue input: inputs) {
        		if (type == null || input.type == null || !input.type.equals(type)) {
        			type = null;
        			break;
        		}
        	}
        	
        	return new SimpleFlowValue(type, null, inputs, true);
    	} else {
    		if (type == null || sw.type == null || !sw.type.equals(type)) {
    			type = null;
    		}
    	}*/
    	
        // return new SimpleFlowValue(type, null, Arrays.asList(new SimpleFlowValue[]{sv, sw}), true);
    	
    	return sv.mergeWith(sw);
    }
    
    public SimpleFlowValue getValue(AbstractInsnNode node) {
    	if (values.containsKey(node)) {
    		return values.get(node);
    	}
    	return null;
    }
}
