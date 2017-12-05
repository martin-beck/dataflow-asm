package org.prettycat.dataflow.asm;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

public class SimpleFlowValue implements Value {
	@Override
	public int hashCode() {
		if (origin != null) {
			return origin.hashCode();
		} else if (isMerge) {
			return inputs.hashCode();
		} else {
			return id;
		}
	}

	public final Type type;
	public final AbstractInsnNode origin;
	public final LinkedHashSet<SimpleFlowValue> inputs;
	public final boolean isMerge;
	public final int id;
	
	private static int uniqueId = 0;

    public SimpleFlowValue(final Type type, final AbstractInsnNode origin, Collection<SimpleFlowValue> inputs, boolean isMerge) {
    	this.id = uniqueId++;
        this.type = type;
        this.origin = origin;
        this.inputs = new LinkedHashSet<SimpleFlowValue>();
        if (inputs != null) { 
        	this.inputs.addAll(inputs);
        }
        this.isMerge = isMerge;
    }

    public Type getType() {
        return type;
    }

    public int getSize() {
        return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 1 : 1;
    }

    public boolean isReference() {
        return type != null
                && (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY);
    }
    
    public static SimpleFlowValue newReturnAddressValue() {
    	return new SimpleFlowValue(Type.VOID_TYPE, null, null, false);
    }
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SimpleFlowValue) {
			SimpleFlowValue other = (SimpleFlowValue)obj;
			if (origin != null) {
				return origin == other.origin;
			}
			if (isMerge) {
				if (!other.isMerge) {
					return false;
				}
				return inputs.equals(other.inputs);
			}
			/* if (type == null && other.type == null) {
				return true;
			} */
			return id == other.id;
		}
		return super.equals(obj);
	}
    
    @Override
    public String toString() {
    	StringBuilder b = new StringBuilder();
    	b.append("<SimpleFlowValue id=");
    	b.append(id);
		b.append(" type=");	
    	b.append(this.type);
    	if (isMerge) {
    		b.append(" merge [");
    		boolean first = true;
    		for (SimpleFlowValue input: inputs) {
    			if (!first) {
    				b.append(", ");
    			}
    			first = false;
    			b.append(input);
    		}
    		b.append("]");
    	} else if (origin != null) {
    		b.append(" origin=");
    		b.append(origin);
    	}
    	b.append(">");
    	return b.toString();
    }
    
    public SimpleFlowValue mergeWith(final SimpleFlowValue other) {
    	// trivial case
    	
    	if (other.equals(this)) {
    		return this;
    	}

    	// System.err.println("merging "+this+" with "+other);
    	
    	// try to determine a reasonable type
    	Type new_type = this.type;
    	if (new_type == null || other.type == null || !new_type.equals(other.type)) {
    		new_type = null;
    	}

    	HashSet<SimpleFlowValue> new_inputs = new HashSet<>();
    	if (isMerge) {
    		new_inputs.addAll(inputs);
    	} else {
    		new_inputs.add(this);
    	}
    	
    	if (other.isMerge) {
    		new_inputs.addAll(other.inputs);
    	} else {
    		new_inputs.add(other);
    	}
    	
    	return new SimpleFlowValue(new_type, null, new_inputs, true);
    }
}