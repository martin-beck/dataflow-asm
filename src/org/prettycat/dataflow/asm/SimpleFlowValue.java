package org.prettycat.dataflow.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.print.attribute.standard.MediaSize.Other;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.analysis.Value;

public class SimpleFlowValue implements Value {
	public final Type type;
	public final AbstractInsnNode origin;
	public final ArrayList<SimpleFlowValue> inputs;
	public final boolean isMerge;
	public final int id;
	
	private static int uniqueId = 0;

    public SimpleFlowValue(final Type type, final AbstractInsnNode origin, Collection<SimpleFlowValue> inputs, boolean isMerge) {
    	this.id = uniqueId++;
        this.type = type;
        this.origin = origin;
        this.inputs = new ArrayList<SimpleFlowValue>();
        if (inputs != null) { 
        	this.inputs.addAll(inputs);
        }
        this.isMerge = isMerge;
    }

    public Type getType() {
        return type;
    }

    public int getSize() {
        return type == Type.LONG_TYPE || type == Type.DOUBLE_TYPE ? 2 : 1;
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
			return this.id == ((SimpleFlowValue)obj).id;
		}
		// TODO Auto-generated method stub
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
    	
    	// try to determine a reasonable type
    	Type new_type = this.type;
    	if (new_type == null || other.type == null || !new_type.equals(other.type)) {
    		new_type = null;
    	}
    	
    	return new SimpleFlowValue(new_type, null, Arrays.asList(new SimpleFlowValue[]{this, other}), true);
    }
}